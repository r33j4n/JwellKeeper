package com.jwellkeeper.jewellery.model;

import com.jwellkeeper.common.model.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "jewellery_types")
public class JewelleryType extends TenantScopedEntity {

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "is_custom", nullable = false)
    private boolean custom;
}
