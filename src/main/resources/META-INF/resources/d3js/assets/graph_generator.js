const DefaultGraphSettings = {
    LINK_FORCE_DISTANCE: 200,
    CHARGE_FORCE_STRENGTH: -100,
    NODE_RADIUS: 30,
    LINK_CURVATURE: 0.15,
    LINK_NODE_INTERSECTION_OFFSET: -5,
};

export class Graph {
    dataNodes;
    dataLinks;
    filteredNodes;
    filteredLinks;
    dataThesoLinks;
    nodes;
    links;
    svg;
    simulation;
    color = d3.scaleOrdinal(d3.schemeCategory10);
    linkForceDistance;
    chargeForceStrength;
    nodeRadius;
    linkCurvature;
    linkNodeIntersectionOffset;
    language;
    dataFormat;

    constructor(data, lang, format, params = DefaultGraphSettings) {
        //Initialisation des données
        console.log(data);

        if(format === "opentheso"){
            this.dataNodes = Array.isArray(data?.nodes) ? data.nodes : []
            this.dataLinks = Array.isArray(data?.relationships) ? data.relationships : []
            this.dataLinks.forEach(function (l) {
                l.source = l.start;
                l.target = l.end;
                l.id = `${l.source}__${l.target}__${l.label}`;
            });
            this.dataThesoLinks = this.dataLinks.filter(
                (value) => value.label === "skos__inScheme"
            );
        } else {
            console.log("format non pris en charge")
        }

        this.language = lang;

        console.log(this.dataNodes);
        console.log(this.dataLinks);
        console.log(this.dataThesoLinks)
        //Initialisation des paramètres
        this.linkForceDistance = params.LINK_FORCE_DISTANCE
            ? params.LINK_FORCE_DISTANCE
            : DefaultGraphSettings.LINK_FORCE_DISTANCE;
        this.chargeForceStrength = params.CHARGE_FORCE_STRENGTH
            ? params.CHARGE_FORCE_STRENGTH
            : DefaultGraphSettings.CHARGE_FORCE_STRENGTH;
        this.nodeRadius = params.NODE_RADIUS
            ? params.NODE_RADIUS
            : DefaultGraphSettings.NODE_RADIUS;
        this.linkCurvature = params.LINK_CURVATURE
            ? params.LINK_CURVATURE
            : DefaultGraphSettings.LINK_CURVATURE;
        this.linkNodeIntersectionOffset = params.LINK_NODE_INTERSECTION_OFFSET
            ? params.LINK_NODE_INTERSECTION_OFFSET
            : DefaultGraphSettings.LINK_NODE_INTERSECTION_OFFSET;

        this.initGraph();
    }

    onNodeDragStart = (e) => {
        if (!e.active) this.simulation.alphaTarget(0.3).restart();
        e.subject.fx = e.subject.x;
        e.subject.fy = e.subject.y;
    };

    //Actualisation de la position du sujet lors du drag
    onNodeDragged = (e) => {
        e.subject.fx = e.x;
        e.subject.fy = e.y;
    };

    //Arret de la simulation de façon progressive
    onNodeDragEnd = (e) => {
        if (!e.active) this.simulation.alphaTarget(0);
        e.subject.fx = null;
        e.subject.fy = null;
    };

    //Lors du zoom sur le graphe
    onGraphZoom(event) {
        d3.select("g").attr("transform", event.transform);
    }

    //Verifie si une relation existe dans le tableau dataThesoLinks avec comme point de départ le noeud passé en paramètres.
    //Si c'est un Thésaurus, retourner l'URI du thésaurus
    isInTheso(node) {

        if (node.labels.includes("skos__ConceptScheme")) {
            return node.id;
        }

        if (node.labels.includes("skos__Collection")) {
            return "skos__Collection";
        }

        const relationships = this.dataThesoLinks.filter(
            (thesoRel) => thesoRel.start == node.id
        );

        if (relationships.length > 0) {
            return relationships[0].end;
        }
        return "no-thesaurus-associated";
    }

    //Filtre un tableau de strings et vérifie la présence d'un tag de langue ('@fr', '@en', etc...) Si ou, retourne la valeur avant le tag
    filterLangArray(array) {
        const values = array.filter((langValue) => {
            const [string, langTag] = langValue.split("@");
            return langTag == this.language;
        });
        if (values.length > 0) {
            return values[0].split("@")[0];
        }
        return "";
    }

    //Création du svg de base
    initGraph() {
        this.svg = d3
            .create("svg")
            .attr("xmlns", "http://www.w3.org/2000/svg")
            .attr("xmlns:xlink", "http://www.w3.org/1999/xlink")
            .attr("viewBox", [
                -window.innerWidth / 2,
                -window.innerHeight / 2,
                window.innerWidth,
                window.innerHeight,
            ])
            .attr("width", window.innerWidth)
            .attr("height", window.innerHeight)
            .call(d3.zoom().on("zoom", this.onGraphZoom));
    }

    //Definition de la tête de flèche pour les liens
    defineLinkArrowHead() {
        this.svg
            .append("defs")
            .append("marker")
            .attr("id", "arrowhead")
            .attr("viewBox", "0 0 20 20")
            .attr("refX", 4)
            .attr("refY", 10)
            .attr("markerWidth", 20)
            .attr("markerHeight", 20)
            .attr("orient", "auto")
            .attr("markerUnits", "userSpaceOnUse")
            .append("path")
            .attr("d", "M 20 0 L 0 10 L 20 20")
            .attr("fill", "#000000");
    }

    createNodes(parent) {
        const nodes = parent
            .append("g")
            .attr("stroke", "#fff")
            .attr("stroke-width", 1.5)
            .attr("style", "z-index:1000")
            .selectAll("g")
            .data(this.filteredNodes)
            .join("g")
            .on("click", (event) =>
                window
                    .open(event.srcElement.__data__.properties.uri, "_blank")
                    .focus()
            )
            .style("cursor", "pointer");

        nodes
            .append("circle")
            .attr("r", (d) =>
                d.labels.includes("skos__ConceptScheme")
                    ? this.nodeRadius * 2
                    : this.nodeRadius
            )

            .attr("fill", (d) =>
                d3.color(this.color(this.isInTheso(d))).brighter(1)
            );

        nodes
            .append("text")
            .text((d) => {
                if (
                    d.labels.includes("skos__Concept") ||
                    d.labels.includes("skos__Collection") ||
                    d.labels.includes("skos__ConceptScheme")
                ) {
                    if (typeof d.properties.skos__prefLabel != "string") {
                        return this.filterLangArray(
                            d.properties.skos__prefLabel
                        );
                    } else {
                        return d.properties.skos__prefLabel;
                    }
                } else {
                    return d.properties.uri;
                }
            })
            .attr("fill", (d) =>
                d3.color(this.color(this.isInTheso(d))).darker(2)
            )
            .attr("stroke", "none")
            .attr("text-anchor", "middle")
            .attr("alignement-baseline", "center")
            .attr("style", "font-family: Helvetica;");

        nodes.call(
            d3
                .drag()
                .on("start", this.onNodeDragStart)
                .on("drag", this.onNodeDragged)
                .on("end", this.onNodeDragEnd)
        );
        return nodes;
    }

    createLinks(parent) {
        const links = parent
            .append("g")
            .selectAll("g")
            .data(this.filteredLinks)
            .join("g");

        links
            .append("path")
            .attr("stroke-width", "3")
            .attr("marker-start", "url(#arrowhead)")
            .attr("id", (d) => d.id)
            .attr("fill", "none")
            .attr("stroke", (d) => this.color(d.label));

        links
            .append("text")
            .attr("text-anchor", "middle")
            //.attr("transform", "translate(0, 6)")
            .attr("style", "pointer-events: none")
            .attr("dy", "-10")
            .append("textPath")
            .text((d) =>
                relationships_dict[d.label]
                    ? relationships_dict[d.label]
                    : d.label
            )
            .attr("xlink:href", (d) => `#${d.id}`)
            .attr("startOffset", "50%")
            .attr("class", "hide-label");

        return links;
    }

    //Initialisation de la simulation des forces
    initSimulation() {
        this.simulation = d3.forceSimulation(this.filteredNodes);

        this.simulation
            .force(
                "link",
                d3
                    .forceLink(this.filteredLinks)
                    .id((d) => d.id)
                    .distance(this.linkForceDistance)
            )
            .force(
                "charge",
                d3.forceManyBody().strength(this.chargeForceStrength)
            )
            .on("tick", () => {
                if (this.links != undefined) {
                    this.links.selectAll("path").attr("d", this.linkArc);
                }

                if (this.nodes != undefined) {
                    this.nodes
                        .selectAll("circle")
                        .attr("cx", (d) => d.x)
                        .attr("cy", (d) => d.y);

                    this.nodes
                        .selectAll("text")
                        .attr("x", (d) => d.x)
                        .attr("y", (d) => d.y);
                }
            });
    }

    linkArc = (d) => {
        const source = d.source;
        const target = d.target;

        // Calculate differences in x and y coordinates
        var dx = target.x - source.x,
            dy = target.y - source.y,
            dr = Math.sqrt(dx * dx + dy * dy);

        // Calculate intersection points with the source circle
        const t1 = this.nodeRadius / dr;
        const x1 = source.x + t1 * dx;
        const y1 = source.y + t1 * dy;

        // Calculate intersection points with the target circle
        const t2 = this.nodeRadius / dr;
        const x2 = target.x - t2 * dx;
        const y2 = target.y - t2 * dy;

        const sx1 = x1 + this.linkNodeIntersectionOffset * (dy / dr);
        const sy1 = y1 - this.linkNodeIntersectionOffset * (dx / dr);

        const sx2 = x2 + this.linkNodeIntersectionOffset * (dy / dr);
        const sy2 = y2 - this.linkNodeIntersectionOffset * (dx / dr);

        const cx = (x1 + x2) / 2 - this.linkCurvature * dy;
        const cy = (y1 + y2) / 2 + this.linkCurvature * dx;

        if (
            isNaN(sx2) ||
            isNaN(sy2) ||
            isNaN(cx) ||
            isNaN(cy) ||
            isNaN(sx1) ||
            isNaN(sy1)
        ) {
            return;
        }

        // Return the SVG path description for a quadratic Bézier curve
        return `M${sx2},${sy2} Q${cx},${cy} ${sx1},${sy1}`;
    };

    render(selectFilterNodesId, selectFilterLinksId, transform) {
        this.defineLinkArrowHead();
        const linksFilter = [];
        const nodesFilter = [];
        document
            .querySelectorAll(`#${selectFilterLinksId} option:checked`)
            .forEach((opt) => linksFilter.push(opt.value));

        document
            .querySelectorAll(`#${selectFilterNodesId} option:checked`)
            .forEach((opt) => nodesFilter.push(opt.value));

        this.filteredNodes = this.dataNodes.filter((node) => {
            let filterWithoutResource = nodesFilter
            if(nodesFilter.includes("Resource")){
                if(node.labels.length == 1){
                    return node.labels.includes("Resource")
                }
                filterWithoutResource = nodesFilter.filter((fil) => fil != "Resource")
            }
            return node.labels.filter((nLabel) => filterWithoutResource.includes(nLabel)).length > 0;

        });

        this.filteredLinks = this.dataLinks.filter((d) =>
            linksFilter.includes(d.label) && this.filteredNodes.filter((node) => node.id == d.start).length > 0 && this.filteredNodes.filter((node) => node.id == d.end).length > 0
        );



        const content = this.svg.append("g").attr("id", "graph-content");

        if (transform != undefined && transform != null) {
            content.attr("transform", transform);
        }
        this.links =
            this.filteredLinks.length == 0
                ? undefined
                : this.createLinks(content);
        this.nodes = this.createNodes(content);

        const alpha = this.simulation == undefined ? 1 : 0.1;

        this.initSimulation();

        this.simulation.alpha(alpha);

        return this.svg.node();
    }
}

const relationships_dict = {
    skos__broader: "Terme Générique",
    skos__narrower: "Terme Spécifique",
    skos__exactMatch: "Alignement Exact",
    skos__related: "Terme Associé",
    ns0__isReplacedBy: "Est replacé par",
    ns0__replaces: "Remplace",
    ns2__memberOf: "Membre de",
    skos__hasTopConcept: "Top Concept",
};
