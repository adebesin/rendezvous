package com.mapr.rendezvous.proxy.db;

import com.mapr.rendezvous.proxy.db.entity.Model;
import com.mapr.rendezvous.proxy.db.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModelService {
    private final ModelRepository repository;

    public Collection<Model> getAllModels() {
        return repository.findAll();
    }

    public Model setPrimaryModel(String modelId, Boolean primary) {
        Optional<Model> opModel = repository.findById(modelId);

        if(opModel.isPresent()) {
            if(primary)
                repository.saveAll(repository.findByPrimaryTrue().stream().peek(model -> model.setPrimary(false))
                        .collect(Collectors.toList()));

            Model model = opModel.get();
            model.setPrimary(primary);
            return repository.save(model);
        }

        return null;
    }

    public Optional<String> getPrimaryId() {
        List<Model> models = repository.findByPrimaryTrue();
        if(models.isEmpty())
            return Optional.empty();
        return Optional.of(models.get(0).getId());
    }

    public void saveModel(Model model) {
        repository.save(model);
    }
}
