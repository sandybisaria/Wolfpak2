package com.wolfpakapp.wolfpak2.leaderboard;

/**
 * OnInteractingCallbacks is an interface for callbacks to be executed when certain Views on the
 * leaderboard are interacted with.
 */
public interface OnInteractingCallbacks {
    void onInteractionStart();
    void onInteractionInProgress();
    void onInteractionFinish();
}
