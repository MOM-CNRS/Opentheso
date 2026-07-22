import { Graph } from "./graph_generator.js";
let generatedGraph;

const urlParams = new URLSearchParams(window.location.search)
const dataUrl = urlParams.get("dataUrl")
const title = urlParams.get("title");
const dataUrlInput = document.querySelector("#data-url");
if (dataUrlInput && dataUrl) {
    dataUrlInput.value = dataUrl.replace(/\s/, "")
}
console.log('URL = ' + dataUrl)
console.log('Titre =' + title)

if (title) {
    const decodedTitle = decodeURIComponent(title); // Décoder les caractères encodés
    console.log(decodedTitle); // Afficher le titre
    document.title = decodedTitle; // Définir le titre de la page
}

if(dataUrl){
    const dataUrlParams = new URLSearchParams(dataUrl.split("?")[1] || "")
    dataUrlParams.forEach((dataUrlParam, index) => console.log(`${index}, ${dataUrlParam}`))
    initGraph()
}

document.querySelector("#settings-button").addEventListener("click", () => {
    document.querySelector("#settings-panel").classList.toggle("show-settings");
});

document.querySelector("#back-button").addEventListener("click", () => {
    window.close();
});
//Event Bouton pour charger les donnée
document.querySelector("#show-data").addEventListener("click", (event) => {
    initGraph()
});

function initGraph(){
    const input = document.querySelector("#data-url");
    const loader = document.getElementById("loader-annel");
    if(loader.style.display === "none"){
        loader.style.display = "flex";
    }
    if (input.value == "" || input.value == undefined) {
        alert("Erreur, il faut renseigner une URL avant d'afficher le graphe");
        return;
    }
    fetch(input.value)
        .then((res) => {
            if (!res.ok) {
                throw new Error(`Erreur HTTP ${res.status} lors du chargement des données du graphe`);
            }
            return res.json();
        })
        .then((data) => {
            if (!data || !Array.isArray(data.nodes) || !Array.isArray(data.relationships)) {
                throw new Error("Réponse graphe invalide (nodes/relationships manquants)");
            }
            const graph = document.querySelector("#graph");
            if (graph.hasChildNodes()) {
                graph.removeChild(graph.firstChild);
            }
            const lang = document.querySelector("#language-input").value;
            if (lang == undefined || lang == "") {
                alert("Erreur, la langue n'est pas définie");
                return;
            }
            generatedGraph = new Graph(data, lang, "opentheso");
            loader.style.display = "none";
            graph.appendChild(
                generatedGraph.render("select-nodes", "select-links")
            );
        })
        .catch((reason) => {
            loader.style.display = "none";
            alert(reason)
        });
}

//Resize du svg lors du resize de l'ecran
window.addEventListener("resize", () => {
    const svg = document.querySelector("#graph").firstChild;
    if(svg === undefined){
        return
    }
    svg.setAttribute("viewBox", [
        -window.innerWidth / 2,
        -window.innerHeight / 2,
        window.innerWidth,
        window.innerHeight,
    ]);
    svg.setAttribute("width", window.innerWidth);
    svg.setAttribute("height", window.innerHeight);
});

//Event checkbox affichage des labels
document
    .querySelector("#show-relationships")
    .addEventListener("change", (event) => {
        const labels = document.querySelectorAll("textPath");
        if (event.currentTarget.checked) {
            labels.forEach((label) => label.classList.remove("hide-label"));
        } else {
            labels.forEach((label) => label.classList.add("hide-label"));
        }
    });

//Event Bouton téléchargement SVG
document.querySelector("#download-svg").addEventListener("click", () => {
    const svg = document.querySelector("#graph").innerHTML;
    let filename = document.querySelector("#download-file-name").value;

    if (filename == undefined || filename == "")
        filename = `export_${new Date().getTime()}`;
    download(svg, "image/svg+xml", `${filename}.svg`);
});

//Event changement filtre noeuds
document.querySelector("#select-nodes").addEventListener("change", (event) => {
    if (generatedGraph != undefined) {
        const graph = document.querySelector("#graph");
        const zoom = document
            .querySelector("#graph-content")
            .getAttribute("transform");

        graph.firstChild.innerHTML = null;
        graph.appendChild(
            generatedGraph.render("select-nodes", "select-links", zoom)
        );
    }
});

//Event changement filtre liens
document.querySelector("#select-links").addEventListener("change", (event) => {
    if (generatedGraph != undefined) {
        const graph = document.querySelector("#graph");
        const zoom = document
            .querySelector("#graph-content")
            .getAttribute("transform");

        graph.firstChild.innerHTML = null;
        graph.appendChild(
            generatedGraph.render("select-nodes", "select-links", zoom)
        );
    }
});

//Fonction téléchargement fichier
function download(content, mimeType, filename) {
    const a = document.createElement("a"); // Create "a" element
    const blob = new Blob([content], { type: mimeType }); // Create a blob (file-like object)
    const url = URL.createObjectURL(blob); // Create an object URL from blob
    a.setAttribute("href", url); // Set "a" element link
    a.setAttribute("download", filename); // Set download filename
    a.click(); // Start downloading
}
