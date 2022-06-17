package tn.antra.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tn.antra.entities.Presentation;
@Repository
public interface PresentationRepository extends JpaRepository<Presentation, Long>{
    public List<Presentation> findAllByOrderByIdDesc();

}
