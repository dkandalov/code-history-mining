(function() {
this.d3c = _.extend(this.d3c || {}, {common: common});
function common() {
    return {

        observable: function(object, eventName) {
            eventName = (eventName === undefined ? "onUpdate" : eventName);
            var listeners = [];
            object[eventName] = function(newListeners) {
                if (_.isArray(newListeners)) listeners = newListeners;
                else listeners = [newListeners];
            };
            return function(update) {
                for (var i = 0; i < listeners.length; i++) {
                    listeners[i](update);
                }
            };
        },
        extendCopyOf: function(object, updatedObject) {
            if (object === undefined) return updatedObject;
            else return _.extend({}, object, updatedObject);
        },
        newHashSet: function(keyFunction) {
            var it = {};
            it.add = function(value) {
                it[keyFunction(value)] = value;
                return it;
            };
            it.addAll = function(values) {
                values.forEach(function(value) {
                    it.add(value);
                });
                return it;
            };
            it.contains = function(value) {
                return it[keyFunction(value)] !== undefined;
            };
            it.contain = function(value) {
                return it.contains(value);
            };
            return it;
        },

        offsetTop: function(element) {
            if (_.isArray(element)) element = element[0][0];
            return pageYOffset + element.getBoundingClientRect().top;
        },
        offsetLeft: function(element) {
            if (_.isArray(element)) element = element[0][0];
            return pageXOffset + element.getBoundingClientRect().left;
        },
        offsetHeight: function(element) {
            if (_.isArray(element)) element = element[0][0];
            return element.getBoundingClientRect().height;
        },
        offsetWidth: function(element) {
            if (_.isArray(element)) element = element[0][0];
            return element.getBoundingClientRect().width;
        },
        styleWidthOf: function(element) {
            return parseInt(element.style("width"));
        },
        removeChildrenOf: function(element) {
            element.selectAll("*").remove();
            return element;
        },

        parseDateBasedCsv: function(rawCsv) {
            var dateFormat = d3.time.format("%d/%m/%Y");
            return d3.csv.parse(rawCsv.trim(), function(row) {
                var result = { date: dateFormat.parse(row.date) };
                _.keys(row).forEach(function(it) {
                    if (it !== "date") result[it] = parseFloat(row[it]);
                    if (_.isNaN(result[it])) result[it] = row[it];
                });
                return result;
            });
        },

        newDataSource: function(data, key) {
            var it = {key: key};
            var notifyListeners = observable(it);
            var categories = _.isEmpty(data) ? [] : _.keys(data[0]).filter(function(it) { return it !== key; });
            it.sendUpdate = function() {
                notifyListeners({
                    data: data,
                    key: key,
                    categories: categories
                });
            };
            return it;
        },
        dataSourceSwitcher: function(dataSources) {
            var index = 0;
            var it = _.clone(dataSources[0]);
            var notifyListeners = observable(it);
            dataSources.forEach(function(it) {
                it.onUpdate(function(update) {
                    update.dataSourceIndex = index;
                    notifyListeners(update);
                });
            });
            it.sendUpdate = function() {
                dataSources[index].sendUpdate();
            };
            it.setDataSourceIndex = function(value) {
                index = value;
                it.sendUpdate();
            };
            return it;
        },
        withDataSourceIndexAsGroup: function(groupFunctions, dataSource) {
            var it = _.clone(dataSource);
            var notifyListeners = observable(it);
            dataSource.onUpdate(function(update) {
                var groupFunction = groupFunctions[update.dataSourceIndex];
                notifyListeners(extendCopyOf(update, {
                    groupByIndex: update.dataSourceIndex,
                    groupFloor: groupFunction.floor,
                    groupNextFloor: groupFunction.nextFloor,
                    groupSize: groupFunction.size,
                    groupFunctions: groupFunctions
                }));
            });
            it.sendUpdate = function() {
                dataSource.sendUpdate();
            };
            it.groupBy = function(value) {
                if (!_.isNumber(value)) {
                    var i = groupFunctions.indexOf(value);
                    if (i === -1) throw new Error("No matching group function for: " + value);
                    value = i;
                }
                it.setDataSourceIndex(value);
            };
            return it;
        },
        logUpdate: function(dataSource) {
            var it = _.clone(dataSource);
            var notifyListeners = observable(it);
            dataSource.onUpdate(function(update) {
                console.log("onUpdate");
                console.log(update);
                notifyListeners(update);
            });
            it.sendUpdate = function() {
                console.log("sendUpdate");
                dataSource.sendUpdate();
            };
            return it;
        },
        withFirstCategories: function(amount, dataSource) {
            var it = _.clone(dataSource);
            var notifyListeners = observable(it);
            dataSource.onUpdate(function(update) {
                notifyListeners(extendCopyOf(update, {
                    categories: _.take(update.categories, amount)
                }));
            });
            it.sendUpdate = function() {
                dataSource.sendUpdate();
            };
            return it;
        },
        withCategoryExclusion: function(dataSource) {
            var excludedCategories = [];
            var it = _.clone(dataSource);
            var notifyListeners = observable(it);
            dataSource.onUpdate(function(update) {
                notifyListeners(extendCopyOf(update, {
                    categories: _.filter(update.categories, function(it) {
                        return excludedCategories.indexOf(it) === -1;
                    })
                }));
            });
            it.sendUpdate = function() {
                dataSource.sendUpdate();
            };
            it.excludeCategory = function(value) {
                excludedCategories.push(value);
                it.sendUpdate();
            };
            it.clearCategoryExclusions = function() {
                excludedCategories = [];
                it.sendUpdate();
            };
            return it;
        },

        autoGroupOnFirstUpdate: function(dataAmountThreshold, dataSource) {
            var ranOnce = false;
            var it = _.clone(dataSource);
            var notifyListeners = observable(it);
            dataSource.onUpdate(function(update) {
                if (ranOnce) {
                    notifyListeners(update);
                    return;
                }
                ranOnce = true;

                if (update.min === undefined) throw new Error("autoGroupOnFirstUpdate() requires min/max key values");

                var min = update.min[update.key];
                var max = update.max[update.key];
                var groupFunction = findGroupFunctionWithValuesAmountBelow(dataAmountThreshold, update.groupFunctions, min, max);
                if (groupFunction === undefined) {
                    groupFunction = _.last(update.groupFunctions);
                }

                dataSource.groupBy(groupFunction);
            });
            it.sendUpdate = function() {
                dataSource.sendUpdate();
            };
            return it;

            function findGroupFunctionWithValuesAmountBelow(threshold, groupFunctions, min, max) {
                return _.find(groupFunctions, function(groupFunction) {
                    var count = 0;
                    var value = min;
                    while (value < max && count < threshold) {
                        value = groupFunction.nextFloor(value);
                        count++;
                    }
                    return count < threshold;
                });
            }
        },

        filteredByPercentile: function(category, dataSource) {
            var percentile = 1.0;
            var lastUpdate;
            var it = _.clone(dataSource);
            var notifyListeners = observable(it);
            dataSource.onUpdate(function(update) {
                lastUpdate = update;
                var filteredData = filter(lastUpdate.data, percentile);
                notifyListeners(extendCopyOf(lastUpdate, {
                    percentile: percentile,
                    data: filteredData
                }));
            });
            it.sendUpdate = function() {
                dataSource.sendUpdate();
            };
            it.setPercentile = function(value) {
                percentile = value;
                it.sendUpdate();
            };
            return it;

            function filter(data, percentile) {
                if (percentile === 1.0) return data;

                var sortedData = _.clone(data).sort(function(a, b){ return a[category] - b[category]; });
                var n = sortedData[Math.round((sortedData.length - 1) * percentile)];
                var threshold = n[category];

                var dataCopy = _.clone(data);
                for (var i = dataCopy.length - 1; i >= 0; i--) {
                    if (dataCopy[i][category] > threshold) dataCopy.splice(i, 1);
                }
                return dataCopy;
            }
        },

        withRowTotal: function(totalCategory, data) {
            var it = _.clone(data);
            var notifyListeners = observable(it);
            data.onUpdate(function(update) {
                update.data.forEach(function(row) {
                    var sum = 0;
                    update.categories.forEach(function(category) {
                        sum += row[category];
                    });
                    row[totalCategory] = sum;
                });
                notifyListeners(extendCopyOf(update, {
                    totalKey: totalCategory
                }));
            });
            it.sendUpdate = function() {
                data.sendUpdate();
            };
            return it;
        },

        withMinMaxKey: function(dataSource) {
            var it = _.clone(dataSource);
            var notifyListeners = observable(it);
            dataSource.onUpdate(function(update) {
                if (update.min === undefined) update.min = {};
                if (update.max === undefined) update.max = {};
                update.min[update.key] = d3.min(update.data, function(row) { return row[update.key]; });
                update.max[update.key] = d3.max(update.data, function(row) { return row[update.key]; });
                notifyListeners(update);
            });
            it.sendUpdate = function() {
                dataSource.sendUpdate();
            };
            return it;
        },

        clampedMin: function(value, minMaxDataSource) {
            return minMaxDataSource.clampMin(value);
        },
        withMinMax: function(category, dataSource) {
            var clampMin;
            var clampMax;

            var it = _.clone(dataSource);
            var notifyListeners = observable(it);
            dataSource.onUpdate(function(update) {
                if (update.min === undefined) update.min = {};
                if (update.max === undefined) update.max = {};

                if (clampMin !== undefined) update.min[category] = clampMin;
                else update.min[category] = d3.min(update.data, function(row) { return row[category]; });

                if (clampMax !== undefined) update.max[category] = clampMax;
                else update.max[category] = d3.max(update.data, function(row) { return row[category]; });

                notifyListeners(update);
            });
            it.sendUpdate = function() {
                dataSource.sendUpdate();
            };
            it.clampMin = function(value) {
                clampMin = value;
                return it;
            };
            it.clampMax = function(value) {
                clampMax = value;
                return it;
            };
            return it;
        },

        withMinMaxOfRow: function(dataSource) {
            var it = _.clone(dataSource);
            var notifyListeners = observable(it);
            dataSource.onUpdate(function(update) {
                var minValue = d3.min(update.data, function(row) {
                    return d3.min(update.categories, function(category) {
                        return row[category];
                    });
                });
                var maxValue = d3.max(update.data, function(row) {
                    return d3.max(update.categories, function(category) {
                        return row[category];
                    });
                });
                if (update.min === undefined) update.min = {};
                if (update.max === undefined) update.max = {};
                update.min["value"] = minValue;
                update.max["value"] = maxValue;
                notifyListeners(update);
            });
            it.sendUpdate = function() {
                dataSource.sendUpdate();
            };
            return it;
        },

        d3time: function(timeIntervals) {
            function asGroupingFunction(interval) {
                return {
                    size: new Date(interval.ceil(new Date(1)).getTime() - interval.floor(new Date(0)).getTime()),
                    floor: interval.floor,
                    nextFloor: function(value) {
                        if (value === null || value === undefined) return value;
                        var ceil = interval.ceil(value);
                        if (value.getTime() < ceil.getTime()) return ceil;
                        else return interval.ceil(new Date(ceil.getTime() + 1));
                    }
                };
            }
            return timeIntervals.map(asGroupingFunction);
        },
        groupedBy: function(groupFunctions, dataSource) {
            var defaultGroupByIndex = 0;
            var groupByIndex = 0;

            var it = _.clone(dataSource);
            var notifyListeners = observable(it);
            dataSource.onUpdate(function(update) {
                var groupSize = groupFunctions[groupByIndex].size;
                var groupFloor = groupFunctions[groupByIndex].floor;
                var groupNextFloor = groupFunctions[groupByIndex].nextFloor;

                var groupedData = (groupByIndex == defaultGroupByIndex ? update.data :
                    groupBy(groupFloor, update.data, update.key, update.categories));

                notifyListeners(extendCopyOf(update, {
                    data: groupedData,
                    groupByIndex: groupByIndex,
                    groupFloor: groupFloor,
                    groupNextFloor: groupNextFloor,
                    groupSize: groupSize,
                    groupFunctions: groupFunctions
                }));
            });
            it.sendUpdate = function() {
                dataSource.sendUpdate();
            };
            it.groupBy = function(value) {
                if (!_.isNumber(value)) {
                    var i = groupFunctions.indexOf(value);
                    if (i === -1) throw new Error("No matching group function for: " + value);
                    value = i;
                }
                groupByIndex = value;
                dataSource.sendUpdate();
            };
            return it;

            function groupBy(groupFunction, data, key, categories) {
                return d3.values(d3.nest()
                    .key(function(row) {
                        return groupFunction(row[key]);
                    })
                    .rollup(function(rows) {
                        var initialValue = {};
                        initialValue[key] = groupFunction(rows[0][key]);
                        categories.forEach(function(it) {
                            initialValue[it] = 0;
                        });

                        return _.reduce(rows, function(result, row) {
                            categories.forEach(function(it) {
                                result[it] = result[it] + row[it];
                            });
                            return result;
                        }, initialValue);
                    })
                    .map(data));
            }
        },

        newTimeScale: function(category) {
            return newScale(category, d3.time.scale());
        },
        newScale: function(category, d3scale) {
            if (d3scale === undefined) d3scale = d3.scale.linear();
            var it = d3scale;
            it.update = function(update) {
                if (update.min === undefined) {
                    throw new Error("newScale() didn't find min/max values for '" + category + "'. " +
                                    "See withMinMax...() functions.");
                }
                it.domain([update.min[category], update.max[category]]);
            };
            var notifyListeners = observable(it);
            it.setDomain = function(extent) {
                it.domain(extent);
                notifyListeners(it);
            };
            // unlike d3 scale this function can also also extract value from data row
            it.getAndApply = function(d) {
                return it(d[category]);
            };
            return it;
        },
        withGroupValueSize: function(uiConfig, scale) {
            var groupSize;

            var it = scale;
            var notifyListeners = observable(it);
            var scaleUpdate = it.update;
            var scaleSetDomain = it.setDomain;
            it.update = function(update) {
                scaleUpdate(update);
                if (update.groupSize === undefined || update.groupNextFloor === undefined) {
                    throw new Error("withGroupValueSize() requires grouping functions. See groupBy() function.")
                }
                groupSize = update.groupSize;
                it.domain([update.min[update.key], update.groupNextFloor(update.max[update.key])]);
                it.valueSize = valueSize(uiConfig, amountOfGroupsIn(it.domain(), groupSize));
            };
            it.setDomain = function(extent) {
                it.valueSize = valueSize(uiConfig, amountOfGroupsIn(extent, groupSize));
                scaleSetDomain(extent);
                notifyListeners(it);
            };
            return it;

            function amountOfGroupsIn(domain, groupSize) {
                var domainSize = domain[1] - domain[0];
                return domainSize / groupSize;
            }
            function nonZero(length) {
                return (length > 0 ? length : 0.0000001);
            }
            function valueSize(uiConfig, amountOfValues) {
                var result = uiConfig.width / nonZero(amountOfValues) - 1;
                result = Math.floor(result);
                if (result > 20) result = result - 1;
                if (result < 1) result = 1;
                return result;
            }
        },

        newControlsPanel: function(root, uiConfig) {
            var panelRoot = root.append("div").style({
                width: uiConfig.width + "px", height: 20 + "px"
            }).attr("class", "controlPanel");

            var placeholderWidth = (uiConfig.margin === undefined ? 0 : uiConfig.margin.left);
            var leftFooter = panelRoot.append("div")
                .style({ float: "left", display: "block", "margin-left": placeholderWidth + "px"})
                .attr("class", "panelWithControls");

            var it = panelRoot.append("div").style({float: "right"}).attr("class", "panelWithControls");

            it.leftFooter = function() {
                return leftFooter;
            };
            return it;
        },

        newCheckBox: function(root, label, callback) {
            var span = root.append("span");
            span.append("label").html(label + "&nbsp;");
            span.append("input").attr("type", "checkbox").on("click", function() {
                callback(this.checked);
            });
        },

        newDropDown: function(root, label, optionLabels, getSelectedIndex, onChange) {
            var span = root.append("span");
            span.append("label").html(label + "&nbsp;");
            var dropDown = span.append("select");
            for (var i = 0; i < optionLabels.length; i++) {
                dropDown.append("option").attr("value", i).html(optionLabels[i]);
            }

            dropDown.on("change", function() {
                onChange(this.value)
            });

            var it = {};
            it.update = function(update) {
                dropDown[0][0].selectedIndex = getSelectedIndex(update);
                return it;
            };
            return it;
        },

        newXAxis: function(root, xScale, uiConfig) {
            if (uiConfig === undefined) uiConfig = {};
            if (uiConfig.xAxisCss === undefined) uiConfig.xAxisCss = "x axis";

            var isBottomLocation = true;
            var label = "";

            var it = d3.svg.axis().scale(xScale).orient("bottom");
            it.update = function() {
                root.selectAll("." + uiConfig.xAxisCss.replace(" ", ".")).remove();
                root.append("g")
                    .attr("class", uiConfig.xAxisCss)
                    .attr("transform", function() {
                        return isBottomLocation ? "translate(0," + uiConfig.height + ")" : "";
                    })
                    .call(it)
                    .append("text")
                    .attr("y", 6)
                    .attr("x", uiConfig.width)
                    .attr("dy", ".71em")
                    .style("text-anchor", "end")
                    .text(label);
            };
            it.onXScaleUpdate = function() {
                root.select(".x.axis").call(it);
            };
            it.label = function(value) { label = value; return it; };
            it.ticksTop = function() { it.orient("top"); return it; };
            it.ticksBottom = function() { it.orient("bottom"); return it; };
            it.locationTop = function() { isBottomLocation = false; return it; };
            it.locationBottom = function() { isBottomLocation = true; return it; };
            return it;
        },

        newYAxis: function(root, yScale, uiConfig) {
            if (uiConfig === undefined) uiConfig = {};
            if (uiConfig.yAxisCss === undefined) uiConfig.yAxisCss = "y axis";

            var valueFormat = function(value) {
                if (_.isDate(value)) return value;
                var s = d3.format(",")(value);
                return s.length < 6 ? s : d3.format("s")(value);
            };
            var label = "";
            var isLeftLocation = true;

            var it = d3.svg.axis().scale(yScale).orient("left").tickFormat(valueFormat);
            it.update = function() {
                root.selectAll("." + uiConfig.yAxisCss.replace(" ", ".")).remove();
                root.append("g")
                    .attr("class", uiConfig.yAxisCss)
                    .attr("transform", function() { return isLeftLocation ? "" : "translate(" + uiConfig.width + ",0)"; })
                    .call(it)
                    .append("text")
                    .attr("transform", "rotate(-90)")
                    .attr("y", 6)
                    .attr("dy", ".71em")
                    .style("text-anchor", "end")
                    .text(label);
            };
            it.label = function(value) { label = value; return it; };
            it.ticksLeft = function() { it.orient("left"); return it; };
            it.ticksRight = function() { it.orient("right"); return it; };
            it.locationLeft = function() { isLeftLocation = true; return it; };
            it.locationRight = function() { isLeftLocation = false; return it; };
            return it;
        },

        newEmptyChartLabel: function(root, svgRoot, uiConfig) {
            var label = root.append("div")
                .html("Unfortunately, there is nothing to show.")
                .style("position", "absolute")
                .style("opacity", 0)
                .attr("class", "tooltip");
            var it = {};
            it.update = function(update) {
                if (update.data.length > 0) {
                    label.style("opacity", 0);
                } else {
                    label.style("opacity", 0.9)
                        .style("top", function(){
                            return offsetTop(svgRoot) + uiConfig.margin.top + (uiConfig.height / 2) - (this.offsetHeight / 2) + "px";
                        })
                        .style("left", function(){
                            return offsetLeft(svgRoot) + uiConfig.margin.left + (uiConfig.width / 2) - (this.offsetWidth / 2) + "px";
                        });
                }
            };
            return it;
        },

        newGroupByDropDown: function(root, groupedData, label, groupNames) {
            return newDropDown(root, label, groupNames,
                function(update) {
                    return update.groupByIndex;
                },
                function(newValue) {
                    groupedData.groupBy(parseInt(newValue));
                }
            );
        },

        helpDescription: function(title, text) {
            return "<strong>" + title + "</strong><br/>" + text;
        },

        addHelpButton: function(root, svgRoot, uiConfig, helpText, helpDescription, settings) {
            if (settings === undefined) settings = {};
            if (settings.tooltipCss === undefined) settings.tooltipCss = "helpTooltip";
            if (settings.buttonCss === undefined) settings.buttonCss = "helpButton";
            if (settings.descriptionCss === undefined) settings.descriptionCss = "helpDescription";
            if (settings.tooltipPad === undefined) settings.tooltipPad = 0;

            var helpButtonImage = "iVBORw0KGgoAAAANSUhEUgAAABoAAAAaCAYAAACpSkzOAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAATZJREFUeNq8VtERwiAMLZ7/skFxAusGHaEjMIIjOEJH6AgdoSPUCWSEboCkBg/RQqBa7t6V6+XlQUgITGtdUAZjTJiP8H4rw1ckByC0BHTcgkMwXYBCGxH0tSDAkawTARxOEjKjMhgzRCyAWwWFUGRaIWIx+WJ+uMYfiLg749+EYmfSGTSYIALnfezM3oSQGCLIQGbKCFe4QqHdDJ7j2k9lsIntyhqG6uTiOOycwxbO/2uozsBmjxVfBmq6NjYc502RPkrUmEORmlHSC52K2Nc5QjKjuJOFRu8epBa33CXGe3DmkNYHIk8xXNmdSLhhkdrEOBF5R+phrsGc3jZ0PXFlZ0NiAJgTOf2r8RGuIP00/bh+KDsSqZdqjlCb2yYqr4bS28RmjW/TVr7p4+Rfzy221QPyIcAAaxoAnJVfnkgAAAAASUVORK5CYII=";

            var tooltipIsVisible = false;
            var tooltip = root.append("div")
                .html(helpText)
                .attr("class", settings.tooltipCss)
                .style({opacity: 0.0, position: "absolute"});

            root.append("img")
                .attr("src", 'data:image/png;base64,' + helpButtonImage)
                .attr("class", settings.buttonCss)
                .on("click", function() {
                    if (!tooltipIsVisible) {
                        tooltip.style({opacity: 1.0})
                            .style("left", (offsetLeft(svgRoot) + uiConfig.margin.left + settings.tooltipPad) + "px")
                            .style("width", (uiConfig.width / 3) + "px")
                            .style("pointer-events", "auto");
                        tooltip.style("top", (offsetTop(svgRoot) + offsetHeight(svgRoot) - offsetHeight(tooltip) - settings.tooltipPad) + "px");
                    } else {
                        tooltip.style({opacity: 0, "pointer-events": "none"});
                    }
                    tooltipIsVisible = !tooltipIsVisible;
                });

            if (helpDescription !== undefined) {
                root.append("div")
                    .attr("class", settings.descriptionCss)
                    .style({color: "#999", display: "inline-block", "vertical-align": "middle"})
                    .html(helpDescription);
            }
        }
    };
}
}());
