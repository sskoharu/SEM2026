package org.app.repositories;

import org.app.schemas.session_object.SessionObject;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionRepository extends CrudRepository<SessionObject, String> {
    // ¡No necesitas escribir nada más!
    // CrudRepository ya te da: save(), findById(), delete(), existsById(), findAll(), etc.
}