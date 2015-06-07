# pe-fp-app

[![Build Status](https://travis-ci.org/evanspa/pe-fp-app.svg)](https://travis-ci.org/evanspa/pe-fp-app)

pe-fp-app is the REST API endpoint for the [fuel purchase system](#about-the-fuel-purchase-system).  The fuel
purchase system consists of 2 tiers:
[client applications](#client-applications) and the server application.
pe-fp-app represents the server application; it exposes a REST API endpoint to
client applications.

pe-fp-app makes use of the
[pe-* Clojure library suite](#pe--clojure-library-suite).  In addition to using
the pe-* library suite, it leverages
[pe-fp-rest](https://github.com/evanspa/pe-fp-rest), which implements the
underlying fuel purchase-specific resources of the REST API.  pe-fp-rest is
built on top of [pe-fp-core](https://github.com/evanspa/pe-fp-core), which
implements the core logic, model and data access functionality of the REST
application.  These tiers are illustrated in the
[component layering](#component-layering) diagram below.

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [About the Fuel Purchase System](#about-the-fuel-purchase-system)
  - [Client Applications](#client-applications)
- [Component Layering](#component-layering)
- [Dependency Graph](#dependency-graph)
- [pe-* Clojure Library Suite](#pe--clojure-library-suite)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## About the Fuel Purchase System

The fuel purchase system provides the ability to record and analyze your vehicle's fuel usage.  In its present form, the fuel purchase system is not terribly useful.  It enables you to collect fuel usage statistics with respect to your vehicles, but none of the analysis / reporting functionality is built out yet.  It exists more as a reference implementation for a set of libraries.
The fuel purchase system is a client/server one.  This repo, *pe-fp-app*,
represents the server-side application of the fuel purchase system.  It exists
as a REST API endpoint for [client applications](#client-applications) of the
system.  The libraries are generic, and thus are not coupled to the fuel
purchase system.

pe-fp-app makes use of the [pe-* Clojure library suite](#pe--clojure-library-suite), as well as [pe-fp-rest](https://github.com/evanspa/pe-fp-rest) (*which uses [pe-fp-core](https://github.com/evanspa/pe-fp-core)*).

### Client Applications

Currently there only exists an iOS client application for the fuel purchase
system: [PEFuelPurchase-App](https://github.com/evanspa/PEFuelPurchase-App).

## Component Layering

The following diagram attempts to illustrate the layered architecture of the
fuel purchase server application.  The various *core* and *rest* libraries
encapsulates the bulk of the application; the core logic, model and data access
functionality.

<img
src="https://github.com/evanspa/pe-fp-app/raw/master/drawings/pe-fp-app-Component-Layers.png">

## Dependency Graph

The following diagram attempts to illustrates the dependencies among the main
components of the fuel purchase server application.

<img
src="https://github.com/evanspa/pe-fp-app/raw/master/drawings/pe-fp-app-Dependency-Graph.png">

## pe-* Clojure Library Suite
The pe-* Clojure library suite is a set of Clojure libraries to aid in the
development of Clojure based applications.
*(Each library is available on Clojars.)*
+ **[pe-core-utils](https://github.com/evanspa/pe-core-utils)**: provides a set
of various collection-related, date-related and other helpers functions.
+ **[pe-jdbc-utils](https://github.com/evanspa/pe-jdbc-utils)**: provides
  a set of helper functions for working with JDBC.
+ **[pe-datomic-utils](https://github.com/evanspa/pe-datomic-utils)**: provides
  a set of helper functions for working with [Datomic](https://www.datomic.com).
+ **[pe-datomic-testutils](https://github.com/evanspa/pe-datomic-testutils)**: provides
  a set of helper functions to aid in unit testing Datomic-enabled functions.
+ **[pe-user-core](https://github.com/evanspa/pe-user-core)**: provides
  a set of functions for modeling a generic user, leveraging PostgreSQL as a
  backend store.
+ **[pe-user-testutils](https://github.com/evanspa/pe-user-testutils)**: a set of helper functions to aid in unit testing
code that depends on the functionality of the pe-user-* libraries
([pe-user-core](https://github.com/evanspa/pe-user-core) and [pe-user-rest](https://github.com/evanspa/pe-user-rest)).
+ **[pe-apptxn-core](https://github.com/evanspa/pe-apptxn-core)**: provides a
  set of functions implementing the server-side core data layer of the
  PEAppTransaction Logging Framework.
+ **[pe-rest-utils](https://github.com/evanspa/pe-rest-utils)**: provides a set
  of functions for building easy-to-version hypermedia REST services (built on
  top of [Liberator](http://clojure-liberator.github.io/liberator/).
+ **[pe-rest-testutils](https://github.com/evanspa/pe-rest-testutils)**: provides
  a set of helper functions for unit testing web services.
+ **[pe-user-rest](https://github.com/evanspa/pe-user-rest)**: provides a set of
  functions encapsulating an abstraction modeling a user within a REST API
  and leveraging PostgreSQL.
+ **[pe-apptxn-restsupport](https://github.com/evanspa/pe-apptxn-restsupport)**:
  provides a set of functions implementing the server-side REST layer of the
  PEAppTransaction Logging Framework.
