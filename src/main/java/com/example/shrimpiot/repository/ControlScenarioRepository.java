package com.example.shrimpiot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.shrimpiot.model.ControlScenario;
import com.example.shrimpiot.model.Pond;

@Repository
public interface ControlScenarioRepository extends JpaRepository<ControlScenario, Long> {
    List<ControlScenario> findByPondAndEnabledTrue(Pond pond);
    List<ControlScenario> findByPondIdAndEnabledTrue(Long pondId);
    List<ControlScenario> findByEnabledTrue();
}
