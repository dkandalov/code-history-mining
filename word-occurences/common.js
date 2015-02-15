function observable(target, eventName) {
    eventName = (eventName === undefined ? "onUpdate" : eventName);
    var listeners = [];
    target[eventName] = function(newListeners) {
        if (_.isArray(newListeners)) listeners = newListeners;
        else listeners = [newListeners];
    };
    return function(update) {
        for (var i = 0; i < listeners.length; i++) {
            listeners[i](update);
        }
    };
}

function extendCopyOf(object, updatedObject) {
    return _.extend({}, object, updatedObject);
}

function offsetTop(element) {
    if (_.isArray(element)) element = element[0][0];
    return pageYOffset + element.getBoundingClientRect().top;
}
function offsetLeft(element) {
    if (_.isArray(element)) element = element[0][0];
    return pageXOffset + element.getBoundingClientRect().left;
}
function offsetHeight(element) {
    if (_.isArray(element)) element = element[0][0];
    return element.getBoundingClientRect().height;
}
function offsetWidth(element) {
    if (_.isArray(element)) element = element[0][0];
    return element.getBoundingClientRect().width;
}

function newHashSet(keyFunction) {
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
}

function newHeader(root, uiConfig, name) {
    var headerSpan = root.append("span").style({display: "block", width: uiConfig.width + "px"});
    headerSpan.append("h2").text(name).style({"text-align": "center"});
}

function addPlaceholderTo(element, width) {
    return element.append("span").html("&nbsp;").style({width: width + "px", display: "inline-block"});
}

function newControlsPanel(root, uiConfig) {
    var panelRoot = root.append("span").style({ display: "block",
        width: uiConfig.width + "px", height: 20 + "px"
    });

    var leftFooter = panelRoot.append("div").style({ float: "left", display: "block"});
    var placeholderWidth = (uiConfig.margin === undefined ? 0 : uiConfig.margin.left);
    addPlaceholderTo(leftFooter, placeholderWidth);

    var it = panelRoot.append("div").style({float: "right"});

    it.addSpace = function() {
        addPlaceholderTo(it, 20);
        return it;
    };
    it.leftFooter = function() {
        return leftFooter;
    };
    return it;
}

function newDropDown(root, label, optionLabels, getSelectedIndex, onChange) {
    root.append("label").html(label);
    var dropDown = root.append("select");
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
}

function helpDescription(title, text) {
    return "<strong>" + title + "</strong><br/>" + text;
}

function addHelpButton(root, svgRoot, uiConfig, helpText, helpDescription, settings) {
    if (settings === undefined) settings = {};
    if (settings.cssClass === undefined) settings.cssClass = "helpTooltip";
    if (settings.tooltipPad === undefined) settings.tooltipPad = 0;

    var helpButtonImage = "iVBORw0KGgoAAAANSUhEUgAAABoAAAAaCAYAAACpSkzOAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAATZJREFUeNq8VtERwiAMLZ7/skFxAusGHaEjMIIjOEJH6AgdoSPUCWSEboCkBg/RQqBa7t6V6+XlQUgITGtdUAZjTJiP8H4rw1ckByC0BHTcgkMwXYBCGxH0tSDAkawTARxOEjKjMhgzRCyAWwWFUGRaIWIx+WJ+uMYfiLg749+EYmfSGTSYIALnfezM3oSQGCLIQGbKCFe4QqHdDJ7j2k9lsIntyhqG6uTiOOycwxbO/2uozsBmjxVfBmq6NjYc502RPkrUmEORmlHSC52K2Nc5QjKjuJOFRu8epBa33CXGe3DmkNYHIk8xXNmdSLhhkdrEOBF5R+phrsGc3jZ0PXFlZ0NiAJgTOf2r8RGuIP00/bh+KDsSqZdqjlCb2yYqr4bS28RmjW/TVr7p4+Rfzy221QPyIcAAaxoAnJVfnkgAAAAASUVORK5CYII=";

    var tooltipIsVisible = false;
    var tooltip = root.append("div")
        .html(helpText)
        .attr("class", settings.cssClass)
        .style({opacity: 0.0, position: "absolute"});

    addPlaceholderTo(root, 10);
    root.append("img")
        .attr("src", 'data:image/png;base64,' + helpButtonImage)
        .attr("width", "15px")
        .attr("class", "helpTooltip")
        .style({opacity: 0.4, "vertical-align": "middle", cursor: "pointer"})
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
    addPlaceholderTo(root, 10);

    if (helpDescription !== undefined) {
        root.append("span")
            .style({color: "#999", display: "inline-block", "vertical-align": "middle"})
            .html(helpDescription);
    }
}
