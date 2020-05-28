ALTER TABLE public.model DROP CONSTRAINT unq_project_id_type_name;

ALTER TABLE public.project_models DROP CONSTRAINT uk_cphei6yijnpnhdym2dckgxnka;
ALTER TABLE public.project_models ADD CONSTRAINT project_models_pk PRIMARY KEY (project_id,models_id);

INSERT INTO project_models
            (project_id,
             models_id)
SELECT project_id,
       id
FROM   model
WHERE  NOT EXISTS (SELECT project_id,
                          models_id
                   FROM   project_models
                   WHERE  project_models.project_id = model.project_id
                          AND project_models.models_id = model.id);

ALTER TABLE public.model DROP COLUMN project_id;
ALTER TABLE public.model ADD 'scope' int NOT NULL DEFAULT 0;
