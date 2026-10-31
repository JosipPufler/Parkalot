package hr.algebra.parkalot.services.db;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public abstract class GeneralCrudService<K, T extends JpaRepository<K, Long>>  {
    T repository;

    protected GeneralCrudService(T repository) {
        this.repository = repository;
    }

    public Boolean existsById(Long id) {
        return repository.existsById(id);
    }

    public List<K> findAll() {
        return repository.findAll();
    }

    public Optional<K> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public K save(K entity) {
        return repository.save(entity);
    }

    @Transactional
    public void delete(K entity) {
        repository.delete(entity);
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
