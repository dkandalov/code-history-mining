(function() {
this.d3c = _.extend(this.d3c || {}, {barCharts: barCharts});
function barCharts() {
	return {
		__init__: function() {
			_.extend(this, d3c.common());
			return this;
		},

		newBarShading: function() {
			var it = {};
			it.update = function(update) {
				if (update.event === "mouseout") {
					update.bar.removeAttribute("style");
				} else {
					update.bar.style.fill = shadeColor(update.bar.parentNode.style.fill, -0.15);
				}
			};
			return it;
		},

		newBarTooltip: function(root, svgRoot, uiConfig, settings, getTooltipText) {
			if (settings === undefined) settings = {};
			if (settings.css === undefined) settings.css = "";
			if (settings.delay === undefined) settings.delay = 1500;
			if (settings.delayBeforeHide === undefined) settings.delayBeforeHide = 100;

			var div = root.append("div")
				.attr("class", settings.css)
				.style("position", "absolute")
				.style("opacity", 0);

			var lastUpdate = null;

			var it = {};
			it.update = function(update) {
				lastUpdate = update;
				if (update.event === "mouseout") {
					// use timeout because when moving between two elements, onMouseOver event for next element
					// can come before onMouseOut for old one, what results in tooltip disappearing
					setTimeout(function() {
						if (lastUpdate.event === "mouseout")
							div.transition().delay(0).style("opacity", 0);
					}, settings.delayBeforeHide);
				} else {
					var x = parseInt(update.bar.getAttribute("x"));
					var y = parseInt(update.bar.getAttribute("y"));
					var width = parseInt(update.bar.getAttribute("width"));
					var height = parseInt(update.bar.getAttribute("height"));

					var left = offsetLeft(svgRoot) + uiConfig.margin.left + x + width + 5;
					if (left > offsetLeft(svgRoot) + uiConfig.margin.left + uiConfig.width) {
						left = offsetLeft(svgRoot) + uiConfig.margin.left + uiConfig.width;
					}
					var top = offsetTop(svgRoot) + uiConfig.margin.top + y + (height / 2);

					div.html(getTooltipText(update))
						.style("left", left + "px")
						.style("top", top + "px");

					// can determine actual height only after adding text
					var actualHeight = offsetHeight(div);
					top = offsetTop(svgRoot) + uiConfig.margin.top + y + (height / 2 - actualHeight / 2);
					div.style("top",  top + "px");

					div.transition().delay(settings.delay).style("opacity", 0.9);
				}
			};
			return it;
		},
        tooltipWithDateKey: function(update) {
            var dateFormat = d3.time.format("%d/%m/%Y");
            var valueFormat = function(n) {
                var s = d3.format(",")(n);
                return s.length < 6 ? s : d3.format("s")(n);
            };
            return ["Value: " + valueFormat(update.value), "Date: " + dateFormat(update.key)].join("<br/>");
        },
        tooltipWithDateKeyAndCategory: function(update) {
            return tooltipWithDateKey(update) + "<br/>Category: " + update.category;
        },

        newBars: function(root, uiConfig, xScale, yScale, idPostfix) {
			idPostfix = (idPostfix === undefined ? "-bars" : "-" + idPostfix);
			var color = uiConfig.color || d3.scale.category20();
            var barWidth = uiConfig.barWidth || "10px";
			var dataStacked;

			root.append("defs").append("clipPath").attr("id", "clip" + idPostfix)
				.append("rect").attr("width", uiConfig.width).attr("height", uiConfig.height).attr("x", 1);

			var it = {};
			var notifyCategoryListeners = observable(it);
			var notifyHoverListeners = observable(it, "onHover");
			it.update = function(update) {
                if (update.dataStacked === undefined) {
                    throw new Error("newBars() requires stackedData property. See withStackedData() function.")
                }
				dataStacked = update.dataStacked;

				root.selectAll(".layer" + idPostfix).remove();

				var layer = root.selectAll(".layer" + idPostfix)
					.data(update.dataStacked)
					.enter().append("g")
					.attr("class", "layer" + idPostfix)
					.style("fill", function(d, i) { return color(i); });

                var rect = layer.selectAll("rect")
					.data(function(d) { return d; })
					.enter().append("rect")
					.attr("clip-path", "url(#clip" + idPostfix + ")")
					.attr("x", function(d) { return xScale(d.x); })
					.attr("y", function(d) { return yScale(d.y0 + d.y); })
					.attr("width", xScale.valueSize || barWidth)
					.attr("height", function(d) { return yScale(d.y0) - yScale(d.y0 + d.y); });

				rect.on("mouseover", function(d) {
					notifyHoverListeners({event: "mouseover", bar: this, value: d.y, key: d.x, category: getCategory(d)});
				}).on("mouseout", function() {
					notifyHoverListeners({event: "mouseout", bar: this});
				});

				var categoryUpdate = [];
				update.dataStacked.forEach(function(it, i) {
					categoryUpdate.push({ category: getCategory(it[0]), color: color(i) });
				});
				notifyCategoryListeners(categoryUpdate);
			};
			it.onXScaleUpdate = function(updatedXScale) {
				xScale = updatedXScale;

				var layer = root.selectAll(".layer" + idPostfix).data(dataStacked);
				layer.selectAll("rect")
					.data(function(d) { return d; })
					.attr("x", function(d) { return xScale(d.x); })
					.attr("width", xScale.valueSize || "20px");
			};
			return it;

			function getCategory(d) {
				return d["category"];
			}
		},

        newXBrush: function(root, uiConfig, xScale, height, yPosition) {
            height = height === undefined ? 50 : height;
            yPosition = yPosition === undefined ? (uiConfig.height + uiConfig.margin.top) : yPosition;

            var brushUiConfig = extendCopyOf(uiConfig, {height: height});
            var brushXScale = withGroupValueSize(uiConfig, newTimeScale("date").nice().rangeRound([0, uiConfig.width]));
            var brushYScale = newScale("_row_total_").range([brushUiConfig.height, 0]);
            var brush = d3.svg.brush().x(brushXScale);

            var g = root.append("g").attr("transform", "translate(0" + "," + yPosition + ")");

            brush.update = function(update) {
                brushXScale.update(update);
                brushYScale.update(update);
                updateXScale();
                updateUI(update);
            };
            brush.on("brush", function() {
                updateXScale();
            });
            return brush;


            function updateUI(update) {
                g.selectAll("g").remove();
                g.selectAll("defs").remove();
                g.selectAll("rect").remove();

                g.append("rect")
                    .attr("width", brushUiConfig.width).attr("height", brushUiConfig.height)
                    .attr("style", "fill:none;stroke:black;stroke-width:1;stroke-opacity:0.1;shape-rendering:crispEdges");

                var bars = newBars(g, brushUiConfig, brushXScale, brushYScale, "brushBars");
                if (update !== null) bars.update(update);

                g.append("g")
                    .attr("class", "x brush")
                    .call(brush)
                    .selectAll("rect")
                    .attr("height", height);
            }

            function updateXScale() {
                var extent = brush.empty() ? brushXScale.domain() : brush.extent();
                xScale.setDomain(extent);
            }
        },


		withStackedData: function(dataSource) {
			var it = _.clone(dataSource);
			var notifyListeners = observable(it);
            dataSource.onUpdate(function(update) {
                var groupedData = groupByCategory(update.data, update.key, update.categories);
                var dataStacked = groupedData.length === 0 ? [] : d3.layout.stack()(groupedData);
                notifyListeners(extendCopyOf(update, {
                    dataStacked: dataStacked
                }));
            });
			it.sendUpdate = function() {
                dataSource.sendUpdate();
            };
			return it;

			function groupByCategory(data, key, categories) {
                return categories.map(function(category) {
                    return data.map(function(row) {
                        return {
                            x: row[key],
                            y: row[category],
                            category: category
                        };
                    })
                });
			}
		},

		newStackedDataSource: function(rawCsv) {
            var dataSource;
            if (_.isArray(rawCsv)) {
                dataSource = dataSourceSwitcher(rawCsv.map(function(it) {
                    return newDataSource(parseDateBasedCsv(it), "date");
                }));
            } else {
                dataSource = newDataSource(parseDateBasedCsv(rawCsv), "date");
            }

            var rowTotal = "_row_total_";
            var stackedDataSource = withStackedData(
                autoGroupOnFirstUpdate(100,
                clampedMin(0, withMinMax(rowTotal,
                withRowTotal(rowTotal,
                withMinMaxKey(
                filteredByPercentile(rowTotal,
                groupedBy(d3time([d3.time.day, d3.time.monday, d3.time.month]),
                dataSource
            ))))))));
            stackedDataSource.rowTotal = rowTotal;
            return stackedDataSource;
		},
        newStackedDataArrayPreGrouped: function(rawCsv, groupFunctions) {
            var dataSource = dataSourceSwitcher(rawCsv.map(function(it) {
                return newDataSource(parseDateBasedCsv(it), "date");
            }));

            var rowTotal = "_row_total_";
            var stackedDataSource = withStackedData(
                autoGroupOnFirstUpdate(100,
                clampedMin(0, withMinMax(rowTotal,
                withRowTotal(rowTotal,
                filteredByPercentile(rowTotal,
                withMinMaxKey(
                withDataSourceIndexAsGroup(groupFunctions,
                dataSource
            ))))))));
            stackedDataSource.rowTotal = rowTotal;
            return stackedDataSource;
		},


        percentileDropDown: function(root, data) {
            var optionLabels = [];
            for (var i = 100; i >= 95; i -= 0.5) {
                optionLabels.push(i);
            }
            newDropDown(root, "Percentile:", optionLabels,
                function() { return 0; },
                function(newValue) {
                    data.setPercentile(+optionLabels[newValue] / 100);
                }
            );
        },

        newGroupIndexDropDown: function(root, stackedData, label, groupNames) {
            return newDropDown(root, label, groupNames,
                function(update) {
                    return update.groupByIndex;
                },
                function(newValue) {
                    stackedData.setDataSourceIndex(newValue);
                }
            );
        },

        newShowMovingAverageCheckBox: function(root, movingAverageLine) {
            return newCheckBox(root, "Moving average:", function(isChecked) {
                movingAverageLine.setVisible(isChecked);
            });
		},

		newMovingAverageLine: function(root, uiConfig, xScale, yScale, idPostfix) {
			idPostfix = (idPostfix === undefined ? "-movingAvg" : "-" + idPostfix);
			var line = d3.svg.line()
				.x(function(d) { return xScale(d.date); })
				.y(function(d) { return yScale(d.mean); });
			root.append("defs").append("clipPath").attr("id", "clip" + idPostfix)
				.append("rect").attr("width", uiConfig.width).attr("height", uiConfig.height).attr("x", 1);

			var movingAverageData = null;
			var isVisible = false;

			function redrawLine() {
				root.selectAll(".line" + idPostfix).remove();
				if (isVisible && movingAverageData !== null) {
					root.append("path")
						.datum(movingAverageData)
						.attr("class", "line" + idPostfix)
						.attr("clip-path", "url(#clip" + idPostfix + ")")
						.attr("d", line);
				}
			}

			var it = {};
			it.update = function(update) {
                var getRowDate = function(it) { return it.date; };
                var getRowValue = function(it) { return it[update.totalKey]; };
                movingAverageData = movingAverageForTimedValues(
                    update.data, update.groupFloor, update.groupNextFloor, getRowDate, getRowValue
                );
				redrawLine();
			};
			it.onXScaleUpdate = function() {
				redrawLine();
			};
			it.setVisible = function(value) {
				isVisible = value;
				redrawLine();
			};
			return it;
		},

		movingAverageForTimedValues: function(data, groupFloor, groupNextFloor, getRowDate, getRowValue, period) {
            if (data === undefined || data.length < 2) return [];
			period = (period === undefined ? Math.round(data.length / 10) : period);
			if (period < 2) return [];

			var firstDate = getRowDate(data[0]);
			var lastDate = getRowDate(data[data.length - 1]);
			var allDates = valuesRange(firstDate, lastDate);
			if (allDates.length < period) return [];

            data = valuesForEachDate(data, allDates, getRowDate, getRowValue);

			var mean = d3.mean(allDates.slice(0, period), function(date){ return data[date]; });
			var result = [{date: allDates[period - 1], mean: mean}];

			for (var i = period; i < allDates.length; i++) {
				var date = allDates[i];
				var dateToExclude = allDates[i - period];
				mean += (data[date] - data[dateToExclude]) / period;
				result.push({date: date, mean: mean});
			}

			return result;

            function valuesRange(from, to) {
                var threshold = 0;
                var result = [];
                from = groupFloor(from);
                while (from < to && threshold++ < 1000000) {
                    result.push(from);
                    from = groupNextFloor(from);
                }
                result.push(from);
                return result;
            }
            function valuesForEachDate(data, datesRange, getDate, getValue) {
                var result = {};
                datesRange.forEach(function(date) { result[date] = 0; });
                data.forEach(function(d) { result[getDate(d)] = getValue(d); });
                return result;
            }
        },


        // based on https://gist.github.com/ZJONSSON/3918369
        newLegend: function(root, position) {
            var it = {};
            it.update = function(items) {
                // always redraw legend so that it is above graph
                root.select(".legend").remove();
                var legend = root.append("g").attr("class", "legend")
                    .attr("transform", "translate(" + position.x + "," + position.y + ")");

                if (items.length === 0) {
                    legend.style("opacity", 0);
                } else {
                    legend.style("opacity", 0.9)
                }

                var box = legend.append("rect").attr("class", "box");
                var itemList = legend.append("g").attr("class", "items");

                itemList.selectAll("text")
                    .data(items)
                    .call(function(d) { d.enter().append("text") })
                    .attr("y", function(d, i) { return i * 1.04 + "em"; })
                    .attr("x", "1em")
                    .text(function(d) { return d.category; });

                itemList.selectAll("circle")
                    .data(items)
                    .call(function(d) { d.enter().append("circle") })
                    .attr("cy", function(d, i) { return i - 0.4 + "em"; })
                    .attr("cx", 0)
                    .attr("r", "0.4em")
                    .style("fill",function(d) { return d.color; });

                var legendPadding = 5;
                var itemListBox = itemList[0][0].getBBox();
                box.attr("x", itemListBox.x - legendPadding)
                    .attr("y", itemListBox.y - legendPadding)
                    .attr("height", itemListBox.height + 2 * legendPadding)
                    .attr("width", itemListBox.width + 2 * legendPadding);
            };
            return it;
        },

        newTotalAmountLabel: function(root, label) {
			var leftFooter = root.append("span");
			leftFooter.append("label").style("color", "#999").html(label);
			var totalAmountLabel = leftFooter.append("label").style("color", "#999").html("");

			var lastUpdate = null;
			var xScale = null;

			var it = {};
			it.update = function(update) {
				lastUpdate = update;
				updateTotalAmount();
			};
			it.onXScaleUpdate = function(updatedXScale) {
				xScale = updatedXScale;
				updateTotalAmount();
			};
			return it;

            function updateTotalAmount() {
                if (lastUpdate === null || xScale === null) return;
                if (lastUpdate.totalKey === undefined || lastUpdate.totalKey === null) {
                    throw new Error("newTotalAmountLabel() didn't find 'totalKey' on update object.")
                }
                var amount = totalValueAmountWithin(xScale.domain(), lastUpdate.data, lastUpdate.key, lastUpdate.totalKey);
                totalAmountLabel.html(formatValue(amount));
            }
            function totalValueAmountWithin(dateRange, data, key, totalKey) {
                var withinDomain = function(d) { return d[key] >= dateRange[0] && d[key] < dateRange[1]; };
                return d3.sum(data.filter(withinDomain), function(it) {
                    return it[totalKey];
                });
            }
            function formatValue(n) {
                var s = d3.format(",")(n);
                return s.length < 3 ? s : d3.format(".3s")(n);
            }
		},

		// originally from http://stackoverflow.com/questions/5560248/programmatically-lighten-or-darken-a-hex-color-and-blend-colors
		shadeColor: function(color, percent) {
			if (color.indexOf("rgb") > -1) {
				var hex = color
					.split("(")[1].split(")")[0].split(",")
					.map(function(x) { return parseInt(x); });
				return doShade(hex[0], hex[1], hex[2], percent);
			} else {
				var f = parseInt(color.slice(1), 16),
					R = f >> 16,
					G = f >> 8 & 0x00FF,
					B = f & 0x0000FF;
				return doShade(R, G, B, percent);
			}

			function doShade(R, G, B, percent) {
				var t = percent < 0 ? 0 : 255;
				var p = percent < 0 ? percent * -1 : percent;
				return "#" + (
					0x1000000 + (Math.round((t - R) * p) + R) *
					0x10000 + (Math.round((t - G) * p) + G) *
					0x100 + (Math.round((t - B) * p) + B)
					).toString(16).slice(1);
			}
		}

	}.__init__();

}
}());
