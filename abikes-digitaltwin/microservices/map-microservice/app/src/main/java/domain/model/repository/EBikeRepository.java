package domain.model.repository;

import domain.model.EBike;

/** Repository port for EBike entities, reusing generic bike operations. */
public interface EBikeRepository extends GenericBikeRepository<EBike> {}
