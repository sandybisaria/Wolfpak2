package com.wolfpakapp.wolfpak2.camera.editor;

import android.graphics.Bitmap;

import java.util.ArrayList;

/**
 * Class to hold undo states during editing
 * @author Roland Fong
 */
public class UndoManager {

    private static ArrayList<Bitmap> screenStates;

    static  {
        screenStates = new ArrayList<Bitmap>();
    }
    /**
     * Initializes Undo Manager
     * @deprecated
     */
    public UndoManager ()   {
        screenStates = new ArrayList<Bitmap>();
    }

    /**
     * Adds screen state to state list
     * @param b the bitmap to add to the undo states
     */
    public static void addScreenState(Bitmap b) {
        if(screenStates.size() >= 50)   {
            screenStates.remove(0);
        }
        screenStates.add(b);
    }

    /**
     * @return the last saved screen state
     */
    public static Bitmap getLastScreenState()  {
        return screenStates.get(screenStates.size() - 1); // returns previous state
    }
    /**
     * Removes the last saved screen state and returns the one before it
     * @return the previous screen state
     */
    public static Bitmap undoScreenState()  {
        // screenStates.get(screenStates.size() - 1).recycle();
        screenStates.remove(screenStates.size() - 1); // removes last saved state
        return screenStates.get(screenStates.size() - 1); // returns previous state
    }

    public static int getNumberOfStates()  {
        return screenStates.size();
    }

    /**
     * Clear the state array
     */
    public static void clearStates()    {
        screenStates.clear();
    }
}
