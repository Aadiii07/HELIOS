package com.helios.backend.observations.domain;

/**
 * Only ACTIVE is emitted in this phase — there is no edit/supersede
 * workflow yet (that belongs with the Evidence/Provenance phase,
 * which handles conflicting values deliberately rather than in-place
 * overwrites). The column exists now so that phase doesn't need a
 * migration just to add a status value.
 */
public enum ObservationStatus {
    ACTIVE
}
