# AssetFlow

[![CI](https://github.com/mohammed-mrabet/assetflow/actions/workflows/ci.yml/badge.svg)](https://github.com/mohammed-mrabet/assetflow/actions/workflows/ci.yml)

**Application EAM (Enterprise Asset Management) avec nomenclatures PLM versionnées**, en **Java 21 / Spring Boot 3 / PostgreSQL**.
Elle relie deux mondes souvent séparés dans l'industrie :

- **PLM** : les articles et leurs **nomenclatures (BOM)** multiniveaux, avec révisions A, B, C… (brouillon → diffusée → obsolète) ;
- **EAM / GMAO** : les **équipements** installés, les **ordres de travail** de maintenance corrective et préventive, le **stock de pièces de rechange** et les **indicateurs de fiabilité** (MTTR, MTBF, disponibilité).

Le lien entre les deux : chaque équipement est rattaché au **modèle PLM** dont il est issu. Ses pièces de rechange se déduisent donc automatiquement de la nomenclature diffusée de ce modèle.

## Fonctionnalités

### PLM : articles et nomenclatures
- Articles de type **ensemble** (avec nomenclature) ou **pièce** (gérée en stock, avec stock minimum)
- **Révisions de nomenclature** : création d'un brouillon (copie de la révision en vigueur), modification, **diffusion** (la révision précédente devient obsolète). Une révision diffusée n'est plus modifiable.
- **Éclatement multiniveau** : quantités cumulées pour une unité (ex. 2 motoréducteurs × 2 roulements + 12 rouleaux × 2 roulements = 28 roulements)
- **Détection de cycles** : un ensemble ne peut pas contenir, même indirectement, un ensemble qui le contient
- **Cas d'emploi** (*where-used*) : dans quels ensembles une pièce est utilisée

### EAM : équipements et maintenance
- Équipements avec criticité (A, B, C), localisation, date de mise en service et état (en service, à l'arrêt, réformé)
- **Ordres de travail** avec un cycle de vie contrôlé : `CREATED → PLANNED → IN_PROGRESS → DONE → CLOSED` (annulation possible avant le démarrage). Toute transition interdite est refusée (HTTP 409).
- Une **panne avec arrêt** met l'équipement à l'arrêt ; la fin de l'intervention le remet en service
- **Consommation de pièces** pendant l'intervention, avec contrôle du stock et liste des pièces sous le stock minimum
- **Maintenance préventive** : plans à périodicité fixe, génération automatique chaque matin (`@Scheduled`, cron configurable) des OT arrivés à échéance. La génération est **idempotente** : pas de doublon si un OT est déjà ouvert.
- **Indicateurs de fiabilité** par équipement et par période : nombre de pannes, temps d'arrêt, MTTR, MTBF, disponibilité
- Verrouillage optimiste (`@Version`) des ordres de travail

## Architecture

```
com.mrabet.assetflow
├── plm/          Item, BomRevision, BomLine, BomExplosion (calculs purs), PlmService, PlmController
├── asset/        Asset, AssetService (pièces de rechange via le modèle PLM), AssetController
├── workorder/    WorkOrder (machine à états), WorkOrderStatus, WorkOrderService, WorkOrderController
├── maintenance/  PreventivePlan, PreventiveMaintenanceService, PreventiveScheduler (@Scheduled)
├── kpi/          ReliabilityKpis (calcul pur), KpiService
├── common/       exceptions métier, gestion des erreurs au format RFC 9457 (ProblemDetail)
└── config/       horloge injectée (Clock) pour des tests reproductibles
```

Choix de conception :
- **Modèle riche** : les règles métier (transitions, stock, révisions) sont dans les entités, pas dans les contrôleurs. Elles sont testées unitairement sans base de données.
- **Calculs isolés** : l'éclatement de nomenclature et les indicateurs sont des fonctions pures, indépendantes de JPA.
- **Schéma géré par Flyway** (`ddl-auto: none`), contraintes d'intégrité en base (clés étrangères, `check`, unicité).
- **DTO en `record`** : les entités ne sortent jamais de la couche service (`open-in-view: false`).
- Erreurs HTTP homogènes : 400 (validation), 404 (inconnu), 409 (règle métier).

## Démarrage

Prérequis : JDK 21, Maven, Docker.

```bash
docker compose up -d                                   # PostgreSQL 16
mvn spring-boot:run -Dspring-boot.run.profiles=demo    # API sur http://localhost:8080 avec des données fictives
```

Le profil `demo` ajoute une ligne de conditionnement **fictive** : deux convoyeurs, leur modèle et sa nomenclature sur deux niveaux, et un plan préventif en retard. Les requêtes d'exemple sont dans [`docs/demo.http`](docs/demo.http).

## API

| Méthode | Ressource | Rôle |
|---|---|---|
| POST / GET | `/api/items` | Créer / lister les articles |
| POST | `/api/items/{code}/receipts` | Entrée en stock |
| GET | `/api/items/below-minimum` | Pièces sous le stock minimum |
| POST | `/api/items/{code}/bom-revisions` | Ouvrir une révision brouillon |
| POST / DELETE | `/api/bom-revisions/{id}/lines` | Ajouter / retirer une ligne |
| POST | `/api/bom-revisions/{id}/release` | Diffuser la révision |
| GET | `/api/items/{code}/bom`, `/explosion`, `/where-used` | Nomenclature, éclatement, cas d'emploi |
| POST / GET | `/api/assets` | Créer / lister les équipements |
| GET | `/api/assets/{id}/spare-parts` | Pièces de rechange issues du modèle PLM |
| GET | `/api/assets/{id}/kpis?from=&to=` | MTTR, MTBF, disponibilité |
| POST / GET | `/api/work-orders` | Déclarer une panne / lister (filtres `status`, `assetId`) |
| POST | `/api/work-orders/{id}/schedule`, `/start`, `/parts`, `/complete`, `/close`, `/cancel` | Cycle de vie de l'OT |
| POST / GET | `/api/preventive-plans` | Plans préventifs |
| POST | `/api/preventive-plans/generate` | Générer les OT préventifs dus |

## Tests

```bash
mvn test      # tests unitaires du domaine (sans base)
mvn verify    # + tests d'intégration de l'API sur PostgreSQL (docker compose up -d avant)
```

- **Tests unitaires** (JUnit 5) :
  - machine à états des OT, arrêt et remise en service, consommation de pièces ;
  - cycle de vie des révisions, numérotation (A → B → … → Z → AA) ;
  - éclatement multiniveau, détection de cycles ;
  - échéances préventives ;
  - calcul exact du MTTR, du MTBF et de la disponibilité.
- **Tests d'intégration** (`*IT`, démarrage complet de l'application sur PostgreSQL migré par Flyway) :
  - parcours nomenclature → éclatement → cas d'emploi ;
  - cycle de vie complet d'un OT avec stock ;
  - génération préventive idempotente ;
  - validation et erreurs 400 / 404 / 409.

La CI GitHub Actions démarre un service PostgreSQL 16, exécute `mvn verify` et archive les rapports de tests.

## Limites et pistes

- Pas d'authentification (Spring Security et rôles technicien / responsable seraient la suite logique)
- Périodicité préventive en jours uniquement (pas encore de déclenchement au compteur d'heures de marche)
- Pas d'interface web : l'API est prévue pour être consommée par un front Angular

## Licence

MIT — Mohammed MRABET
