package com.rabbitcare.bunny_org_app.repository;

import com.rabbitcare.bunny_org_app.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {}
