package ca.usask.cs.tonesetandroid.Control;

/**
 * An interface defining any class that changes its state based on the content of a Model, and can be notified by the
 * Model when a change in state occurs.
 *
 * @author alexscott
 */
public interface ModelListener {
    public void modelChanged();
}
