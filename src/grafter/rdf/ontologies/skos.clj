(ns grafter.rdf.ontologies.skos
  (:use [grafter.rdf.ontologies.util]))

(def skos (prefixer "http://www.w3.org/2004/02/skos/core#"))

(def skos:ConceptScheme (skos "ConceptScheme"))
(def skos:hasTopConcept (skos "hasTopConcept"))

(def skos:Concept (skos "Concept"))
(def skos:inScheme (skos "inScheme"))
(def skos:topConceptOf (skos "topConceptOf"))

(def skos:prefLabel (skos "prefLabel"))
(def skos:definition (skos "definition"))
(def skos:notation (skos "notation"))
