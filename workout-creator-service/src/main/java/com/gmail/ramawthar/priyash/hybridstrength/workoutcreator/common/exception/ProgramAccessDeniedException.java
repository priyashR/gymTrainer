package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception;

/**
 * Thrown when a user attempts to access a program that does not exist or
 * belongs to a different user. Maps to 403 Forbidden — intentionally does
 * not distinguish "not found" from "not owned" to avoid leaking resource existence.
 */
public class ProgramAccessDeniedException extends RuntimeException {

    public ProgramAccessDeniedException() {
        super("Program not found or access denied");
    }
}
