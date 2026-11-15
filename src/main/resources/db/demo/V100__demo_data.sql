-- Données FICTIVES de démonstration : une ligne de conditionnement
insert into item (code, name, type, stock_quantity, min_stock) values
    ('MOD-CONV-200', 'Convoyeur à bande 200 (modèle)', 'ASSEMBLY', 0, 0),
    ('ASM-MOT-075',  'Motoréducteur 0,75 kW',          'ASSEMBLY', 0, 0),
    ('PRT-BELT-200', 'Bande transporteuse 200 mm',     'PART',     2, 1),
    ('PRT-ROLL-50',  'Rouleau porteur Ø50',            'PART',    24, 10),
    ('PRT-BRG-6204', 'Roulement 6204-2RS',             'PART',     6, 8),
    ('PRT-SEAL-20',  'Joint à lèvre 20x35',            'PART',    10, 4);

insert into bom_revision (item_id, revision, status, released_at)
select id, 'A', 'RELEASED', now() from item where code in ('MOD-CONV-200', 'ASM-MOT-075');

insert into bom_line (bom_id, child_item_id, quantity)
select b.id, c.id, q.qty
from (values ('MOD-CONV-200', 'ASM-MOT-075', 1),
             ('MOD-CONV-200', 'PRT-BELT-200', 1),
             ('MOD-CONV-200', 'PRT-ROLL-50', 12),
             ('ASM-MOT-075', 'PRT-BRG-6204', 2),
             ('ASM-MOT-075', 'PRT-SEAL-20', 1)) as q(parent, child, qty)
join item p on p.code = q.parent
join bom_revision b on b.item_id = p.id and b.status = 'RELEASED'
join item c on c.code = q.child;

insert into asset (code, name, location, criticality, status, commissioned_on, model_item_id)
select 'CONV-01', 'Convoyeur entrée ligne 1', 'Atelier conditionnement – Ligne 1', 'A', 'IN_SERVICE', date '2024-03-01', id
from item where code = 'MOD-CONV-200';

insert into asset (code, name, location, criticality, status, commissioned_on, model_item_id)
select 'CONV-02', 'Convoyeur sortie ligne 1', 'Atelier conditionnement – Ligne 1', 'B', 'IN_SERVICE', date '2024-03-01', id
from item where code = 'MOD-CONV-200';

insert into preventive_plan (asset_id, title, interval_days, last_done_on, active)
select id, 'Graissage et contrôle des roulements', 30, current_date - 35, true from asset where code = 'CONV-01';
