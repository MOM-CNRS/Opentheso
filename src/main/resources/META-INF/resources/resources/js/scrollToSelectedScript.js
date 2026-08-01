function srollToSelected() {
    var treeWidget = PrimeFaces.widgets["treeWidget"];
    if (!treeWidget || !treeWidget.jq) {
        return;
    }
    var selectedElement = treeWidget.jq.find('.ui-state-highlight').first();
    if (!selectedElement.length || selectedElement.position() === undefined) {
        return;
    }
    var scrollPanel = treeWidget.jq;
    scrollPanel.scrollTop(scrollPanel.scrollTop() + selectedElement.position().top - 40);
}

function srollGroupToSelected() {
    var treeGroupWidget = PrimeFaces.widgets["groupWidget"];
    if (!treeGroupWidget || !treeGroupWidget.jq) {
        return;
    }
    var selectedElement = treeGroupWidget.jq.find('.ui-state-highlight').first();
    if (!selectedElement.length || selectedElement.position() === undefined) {
        return;
    }
    var scrollPanel = treeGroupWidget.jq;
    scrollPanel.scrollTop(scrollPanel.scrollTop() + selectedElement.position().top - 40);
}
