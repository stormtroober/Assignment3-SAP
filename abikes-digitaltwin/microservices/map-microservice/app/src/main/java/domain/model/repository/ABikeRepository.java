package domain.model.repository;

import domain.model.ABike;

public interface ABikeRepository extends GenericBikeRepository<ABike> {
  // This interface extends GenericBikeRepository with the specific type ABike.
  // It can be used to define any additional methods specific to ABike if needed.
}
