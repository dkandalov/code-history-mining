(function() {
this.d3c = _.extend(this.d3c || {}, {graphs: graphs});
function graphs() {
    return {
        __init__: function() {
            _.extend(this, d3c.common());
            return this;
        },

        convertIndexToReferencesLinks: function(graphData) {
            graphData.links.forEach(function(link) {
                var sourceIndex = link.source;
                var targetIndex = link.target;
                link.source = graphData.nodes[sourceIndex];
                link.target = graphData.nodes[targetIndex];
            });
            return graphData;
        },

        newGraph: function(graphData) {
            graphData.originalNodes = graphData.nodes;
            graphData.originalLinks = graphData.links;
            graphData.searchIndex = createNodeSearchIndex(graphData);
            graphData.extendWith = createExtendWith(graphData);
            updateLinkStrengthExtent();

            var it = {};
            var notifyListeners = observable(it);
            it.sendUpdate = function() {
                restoreOriginalGraphData();
                updateLinkStrengthExtent();
                graphData.searchIndex.updateIndex();

                notifyListeners(graphData);
            };
            return it;

            function updateLinkStrengthExtent() {
                var min = Number.MAX_VALUE;
                var max = 0;
                graphData.links.forEach(function(link) {
                    if (link.value < min) min = link.value;
                    if (link.value > max) max = link.value;
                });
                graphData.linkStrengthExtent = {min: min, max: max};
            }

            function restoreOriginalGraphData() {
                graphData.nodes = graphData.originalNodes;
                graphData.links = graphData.originalLinks;
            }

            function createExtendWith(graphData) {
                return function(newAttributes) {
                    var newGraphData = extendCopyOf(graphData, newAttributes);
                    newGraphData.searchIndex = createNodeSearchIndex(newGraphData);
                    newGraphData.extendWith = createExtendWith(newGraphData);
                    return newGraphData;
                };
            }
        },

        createNodeSearchIndex: function(graphData) {
            var quickFindIndex;

            var it = {};
            it.areConnected = function(node1, node2) {
                if (node1 === null || node2 === null) return false;
                return quickFindIndex.areConnected(node1.index, node2.index);
            };
            it.findNodesInClusterWith = function(node) {
                return graphData.nodes.filter(function(it) {
                    return quickFindIndex.areConnected(it.index, node.index);
                });
            };
            it.neighborsOf = function(node) {
                var result = [node];
                graphData.links.forEach(function(it) {
                    if (it.source === node) result.push(it.target);
                    else if (it.target === node) result.push(it.source);
                });
                return result;
            };
            it.updateIndex = function() {
                for (var i = 0; i < graphData.nodes.length; i++) {
                    graphData.nodes[i].index = i;
                }
                quickFindIndex = newQuickFindIndex(graphData.nodes.length);
                graphData.links.forEach(function (link) {
                    quickFindIndex.connect(link.source.index, link.target.index);
                });
            };
            it.updateIndex();
            return it;
        },

        newQuickFindIndex: function(size) {
            var connections = new Array(size);
            for (var i = 0; i < connections.length; i++) {
                connections[i] = i;
            }
            function rootOf(p) {
                return connections[p] === p ? p : rootOf(connections[p]);
            }

            var it = {};
            it.areConnected = function(p1, p2) {
                return connections[p1] === connections[p2];
            };
            it.connect = function(p1, p2) {
                var p1Root = rootOf(connections[p1]);
                var p2Root = rootOf(connections[p2]);
                for (var i = 0; i < connections.length; i++) {
                    if (connections[i] === p1Root) connections[i] = p2Root;
                }
            };
            return it;
        },

        withNodeSelection: function(graph, selectionCallback) {
            var selectedNode = null;
            var isSelectionUpdate = false;

            var it = _.clone(graph);
            var notifyListeners = observable(it);
            graph.onUpdate(function(graphData) {
                graphData.nodes.forEach(function(it) { it.selected = false; });
                var selectedNodes = [];
                if (selectedNode !== null) {
                    selectedNodes = selectionCallback(selectedNode, graphData);
                    selectedNodes.forEach(function(it) { it.selected = true; });
                }

                notifyListeners(graphData.extendWith({
                    isSelectionUpdate: isSelectionUpdate,
                    selectedNodes: selectedNodes
                }));

                isSelectionUpdate = false;
            });
            it.sendUpdate = function() {
                graph.sendUpdate();
            };
            it.selectRelatedNodes = function(node) {
                selectedNode = node;
                isSelectionUpdate = true;
                it.sendUpdate();
            };
            it.clearNodeSelection = function() {
                it.selectRelatedNodes(null);
                return it;
            };
            return it;
        },

        withNodeNeighborsSelection: function(graph) {
            return withNodeSelection(graph,
                function(selectedNode, graphData) {
                    return graphData.searchIndex.neighborsOf(selectedNode);
                }
            );
        },

        withNodeClusterSelection: function(graph) {
            return withNodeSelection(graph,
                function(selectedNode, graphData) {
                    return graphData.searchIndex.findNodesInClusterWith(selectedNode);
                });
        },

        withRemovedNodes: function(graph) {
            var removedNodes = [];
            var removeClusterWithNode = null;

            var it = _.clone(graph);
            var notifyListeners = observable(it);
            graph.onUpdate(function(graphData) {
                removeNodesFrom(graphData, removedNodes);

                if (removeClusterWithNode !== null) {
                    var nodes = graphData.searchIndex.findNodesInClusterWith(removeClusterWithNode);
                    removedNodes = removedNodes.concat(nodes);
                    removeNodesFrom(graphData, nodes);
                    removeClusterWithNode = null;
                }

                notifyListeners(graphData);
            });
            it.sendUpdate = function() {
                graph.sendUpdate();
            };
            it.removeNode = function(node) {
                removedNodes.push(node);
                it.sendUpdate();
            };
            it.removeNodesInClusterWith = function(node) {
                removeClusterWithNode = node;
                it.sendUpdate();
            };
            it.undoNodeRemoval = function() {
                if (removedNodes.length === 0) return;
                removedNodes = [];
                it.sendUpdate();
            };
            return it;

            function removeNodesFrom(graphData, removedNodes) {
                graphData.nodes = _.difference(graphData.nodes, removedNodes);
                graphData.links = graphData.links.filter(function(link) {
                    return _.contains(graphData.nodes, link.source) &&
                        _.contains(graphData.nodes, link.target);
                });
                graphData.searchIndex.updateIndex();
            }
        },

        withNodeClusterSizeFilter: function(graph) {
            var minNodeClusterSize = 2;

            var it = _.clone(graph);
            var notifyListeners = observable(it);
            graph.onUpdate(function(graphData) {
                graphData.nodes = nodesInClustersLargerThan(minNodeClusterSize, graphData);
                graphData.links = graphData.links.filter(function(link) {
                    return _.contains(graphData.nodes, link.source) &&
                        _.contains(graphData.nodes, link.target);
                });
                graphData.searchIndex.updateIndex();

                notifyListeners(graphData.extendWith({
                    minNodeClusterSize: minNodeClusterSize
                }));
            });
            it.sendUpdate = function() {
                graph.sendUpdate();
            };
            it.setMinNodeClusterSize = function(value) {
                minNodeClusterSize = value;
                it.sendUpdate();
            };
            return it;

            function nodesInClustersLargerThan(minClusterSize, graphData) {
                var result = [];
                var visitedNodes = newHashSet(function(node) {
                    return node.name;
                });

                graphData.nodes.forEach(function (node) {
                    if (visitedNodes.contain(node)) return;

                    var connectedNodes = graphData.searchIndex.findNodesInClusterWith(node);
                    if (connectedNodes.length >= minClusterSize) {
                        result = result.concat(connectedNodes);
                    }

                    visitedNodes.addAll(connectedNodes);
                });
                return result;
            }
        },

        withLinkStrengthFilter: function(graph) {
            var minLinkStrength = -1;

            var it = _.clone(graph);
            var notifyListeners = observable(it);
            graph.onUpdate(function(graphData) {
                graphData.links = graphData.links.filter(function(link) {
                    return link.value >= minLinkStrength;
                });
                graphData.searchIndex.updateIndex();

                notifyListeners(graphData.extendWith({
                    minLinkStrength: minLinkStrength
                }));
            });
            it.sendUpdate = function() {
                graph.sendUpdate();
            };
            it.setMinLinkStrength = function(value) {
                minLinkStrength = value;
                it.sendUpdate();
            };
            return it;
        },

        adjustFilteringTillNodeAmountIsLessThan: function(desiredAmountOfNodes, graph) {
            var enforcedMinLinkStrength = false;
            var minLinkStrength = 7;

            graph.onUpdate(function(graphData) {
                if (graphData.nodes.length <= desiredAmountOfNodes) {
                    if (minLinkStrength > graphData.linkStrengthExtent.min && !enforcedMinLinkStrength) {
                        enforcedMinLinkStrength = true;
                        graph.setMinLinkStrength(minLinkStrength);
                    }
                    return;
                }

                minLinkStrength += 2; // this is a speculation to make search for optimal link strength faster
                graph.setMinLinkStrength(minLinkStrength);

                if (graphData.nodes.length === 0) {
                    minLinkStrength--;
                }
            });
            graph.sendUpdate();

            return {
                minLinkStrength: minLinkStrength
            };
        },


        newClusterSizeDropDown: function(root, graph, label) {
            var optionLabels = [];
            for (var i = 2; i <= 10; i++) {
                optionLabels.push(i);
            }
            return newDropDown(root, label, optionLabels,
                function(graphData) {
                    return graphData.minNodeClusterSize - 2;
                },
                function(newValue) {
                    graph.setMinNodeClusterSize(parseInt(newValue) + 2);
                }
            );
        },

        newLinkStrengthDropDown: function(root, graph, graphData, label) {
            var optionLabels = [];
            var min = graphData.linkStrengthExtent.min;
            var max = graphData.linkStrengthExtent.max;
            for (var i = min; i < max; i++) {
                optionLabels.push(i);
            }
            return newDropDown(root, label, optionLabels,
                function(graphData) {
                    return graphData.minLinkStrength - min;
                },
                function(newValue) {
                    graph.setMinLinkStrength(parseInt(newValue) + min);
                }
            );
        },

        newGravityDropDown: function(root, forceField, label, gravityOptionLabels) {
            return newDropDown(root, label, gravityOptionLabels,
                function(gravityIndex) {
                    return gravityIndex;
                },
                function(newValue) {
                    forceField.setGravityIndex(parseInt(newValue));
                }
            );
        },

        newShowFilePathCheckBox: function(root, listeners) {
            return newCheckBox(root, "Show file path:", function(isChecked) {
                listeners.forEach(function(it) {
                    it.setShowFilePath(isChecked);
                });
            })
        },

        newSelectedNodesLabel: function(root, svgRoot, settings) {
            if (settings === undefined) settings = {};
            if (settings.cssClass === undefined) settings.cssClass = "selectedNodesLabel";

            var lastNodes = null;
            var showFilePath = false;

            var localRoot = root.append("div");

            var it = {};
            it.update = function(graphData) {
                var nodesByGroup = d3.nest().key(function(node){ return node.group; }).map(graphData.selectedNodes);
                var svgPos = svgRoot[0][0];
                var svgOffsetTop = offsetTop(svgRoot);

                localRoot.selectAll("div").remove();
                _.keys(nodesByGroup).forEach(function(key) {
                    var nodes = nodesByGroup[key];
                    var selectedNodesLabel = localRoot.append("div")
                        .attr("class", settings.cssClass)
                        .style("position", "absolute")
                        .style("left", offsetLeft(svgPos) + 3 + "px")
                        .style("top", svgOffsetTop + 3 + "px")
                        .html(nodes.map(function (d) {
                            if (showFilePath) return d.name;
                            else return dropFilePath(d.name);
                        }).sort().join("<br/>"))
                        .style("opacity", (nodes.length > 0 ? 0.8 : 0));

                    svgOffsetTop = offsetTop(selectedNodesLabel) + offsetHeight(selectedNodesLabel);
                });
                lastNodes = graphData.selectedNodes;
            };
            it.setShowFilePath = function(value) {
                showFilePath = value;
                if (lastNodes !== null) {
                    it.update({selectedNodes: lastNodes});
                }
            };
            return it;

            function dropFilePath(fileName) {
                var i = fileName.lastIndexOf("/");
                return (i === -1) ? fileName : fileName.substring(i + 1)
            }
        },

        newNodeTooltip: function(root, svgRoot, settings) {
            if (settings === undefined) settings = {};
            if (settings.cssClass === undefined) settings.cssClass = "nodeTooltip";

            var lastNode = null;
            var showFilePath = false;

            var tooltip = root.append("div")
                .attr("class", settings.cssClass)
                .style("opacity", 0);

            var it = {};
            it.update = function(node) {
                if (node === null) {
                    tooltip.style("opacity", .0);
                    lastNode = null;
                } else {
                    tooltip.html(showFilePath ? node.name : dropFilePath(node.name))
                        .style("opacity", .9)
                        .style("position", "absolute")
                        .style("left", offsetLeft(svgRoot) + node.x + 18 + "px")
                        .style("top", offsetTop(svgRoot) + node.y - 18 + "px");
                    lastNode = node;
                }
            };
            it.setShowFilePath = function(value) {
                showFilePath = value;
                if (lastNode !== null) {
                    it.update(lastNode);
                }
            };
            return it;

            function dropFilePath(fileName) {
                var i = fileName.lastIndexOf("/");
                return (i === -1) ? fileName : fileName.substring(i + 1)
            }
        },

        newGraphForceFieldClickHandler: function(graph) {
            var it = {};
            it.update = function(node) {
                var shouldRemoveNode = d3.event.altKey && !d3.event.shiftKey && node !== null;
                var shouldRemoveNodeCluster = d3.event.altKey && d3.event.shiftKey && node !== null;
                var shouldUndoNodeRemoval = (d3.event.altKey || d3.event.shiftKey) && node === null;

                if (shouldRemoveNode) {
                    graph.clearNodeSelection();
                    graph.removeNode(node);
                } else if (shouldRemoveNodeCluster) {
                    graph.clearNodeSelection();
                    graph.removeNodesInClusterWith(node);
                } else if (shouldUndoNodeRemoval) {
                    graph.clearNodeSelection();
                    graph.undoNodeRemoval();
                } else {
                    graph.selectRelatedNodes(node);
                }
            };
            return it;
        },

        addAreaFrame: function(svgRoot, uiConfig) {
            svgRoot.append("rect")
                .attr("width", uiConfig.width).attr("height", uiConfig.height)
                .style({fill: "none", stroke: "#AAAAAA"});
        },

        newGraphForceField: function(root, uiConfig, settings) {
            if (settings === undefined) settings = {};
            if (settings.nodeCssClass === undefined) settings.nodeCssClass = "node";
            if (settings.linkCssClass === undefined) settings.linkCssClass = "link";

            var svgLinks = root.append("g");
            var svgNodes = root.append("g");
            var linkElements = [];
            var nodeElements = [];
            var handledByNode = false;

            var gravityTypes = [-320, -120, -50];
            var gravityIndex = 1;
            var lastGraphData;

            var it = {};
            var notifyListeners = observable(it);
            var notifyHoverListeners = observable(it, "onNodeHover");
            var notifyClickListeners = observable(it, "onNodeClick");

            var force = d3.layout.force()
                .charge(function(){ return gravityTypes[gravityIndex]; })
                .linkDistance(30)
                .size([uiConfig.width, uiConfig.height]);

            it.update = function(graphData) {
                if (graphData.isSelectionUpdate) {
                    nodeElements = updateNodes(svgNodes, force, graphData.nodes);
                    nodeElements.data(graphData.nodes, byNodeName).attr("r", function(d){ return d.selected ? 7 : 5; });
                    return;
                }

                linkElements = updateLinks(svgLinks, graphData.links);
                nodeElements = updateNodes(svgNodes, force, graphData.nodes);

                force.nodes(graphData.nodes).links(graphData.links).start();
                force.on("tick", onForceTick);

                lastGraphData = graphData;
                notifyListeners(gravityIndex);
            };
            it.setGravityIndex = function(index) {
                gravityIndex = index;
                it.update(lastGraphData);
            };
            root.on("click", function () {
                if (handledByNode) {
                    handledByNode = false;
                    return;
                }
                notifyClickListeners(null);
                notifyHoverListeners(null);
            });
            return it;

            function onForceTick() {
                linkElements
                    .attr("x1", function(d) { return d.source.x; })
                    .attr("y1", function(d) { return d.source.y; })
                    .attr("x2", function(d) { return d.target.x; })
                    .attr("y2", function(d) { return d.target.y; });
                nodeElements
                    .attr("cx", function(d) { return d.x; })
                    .attr("cy", function(d) { return d.y; });
            }

            function updateNodes(svg, force, graphNodes) {
                svg.selectAll(".node")
                    .data(graphNodes, byNodeName)
                    .enter().append("circle")
                    .attr("class", settings.nodeCssClass)
                    .attr("r", function(d){ return d.selected ? 7 : 5; })
                    .style("fill", function (d) { return uiConfig.color(d.group); })
                    .call(force.drag)
                    .on("click", function(node) {
                        notifyClickListeners(node);
                        notifyHoverListeners(null);
                        handledByNode = true;
                    })
                    .on("mouseover", function(node) { notifyHoverListeners(node); })
                    .on("mouseout", function() { notifyHoverListeners(null) });
                svg.selectAll(".node")
                    .data(graphNodes, byNodeName)
                    .exit().remove();
                return svg.selectAll(".node");
            }

            function updateLinks(svg, graphLinks) {
                svg.selectAll(".link")
                    .data(graphLinks, byLinkState)
                    .enter().append("line")
                    .attr("class", settings.linkCssClass)
                    .style("stroke-width", function (d) { return Math.sqrt(d.value); });
                svg.selectAll(".link")
                    .data(graphLinks, byLinkState)
                    .exit().remove();
                return svg.selectAll(".link");
            }

            function byNodeName(node) { return node.name; }
            function byLinkState(link) {
                return "" + link.source.index + "-" + link.target.index;
            }
        },

        newEmptyGraphLabel: function(root, svgRoot, settings) {
            if (settings === undefined) settings = {};
            if (settings.cssClass === undefined) settings.cssClass = "nodeTooltip";

            var label = root.append("div")
                .attr("class", settings.cssClass)
                .html("Unfortunately, there is nothing to show.")
                .style("position", "absolute");

            var it = {};
            it.update = function(graphData) {
                if (graphData.nodes.length > 0) {
                    label.style("opacity", 0);
                } else {
                    label.style("opacity", 0.9)
                        .style("top", function (){ return offsetTop(svgRoot) + (offsetHeight(svgRoot) / 2) - (offsetHeight(this) / 2) + "px"; })
                        .style("left", function (){ return offsetLeft(svgRoot) + (offsetWidth(svgRoot) / 2) - (offsetWidth(this) / 2) + "px"; });
                }
            };
            return it;
        },


        // this is intended to make graph colors look better on projector in case default colors are too pale to see
        enableDarkColorsShortcut: function() {
            document.onkeydown = function(e) {
                e = window.event || e;
                if (String.fromCharCode(e.keyCode) === 'D') {
                    d3.selectAll(".link")[0].forEach(function(link) {
                        link.style["stroke"] = "#333000";
                        link.style["stroke-opacity"] = 0.8;
                    });
                    d3.selectAll(".node")[0].forEach(function(node) {
                        if (node.style["fill"] === "#1f77b4") node.style["fill"] = "#1033a2";
                        else if (node.style["fill"] === "#aec7e8") node.style["fill"] = "#cc6666";
                    });
                }
            };
        }

    }.__init__();
}
}());
