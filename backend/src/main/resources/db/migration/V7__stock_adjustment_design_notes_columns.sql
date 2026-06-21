ALTER TABLE stock_adjustments
    ADD COLUMN before_design_name VARCHAR(160) NULL AFTER before_weight,
    ADD COLUMN after_design_name VARCHAR(160) NULL AFTER before_design_name,
    ADD COLUMN before_notes VARCHAR(1000) NULL AFTER after_design_name,
    ADD COLUMN after_notes VARCHAR(1000) NULL AFTER before_notes;
