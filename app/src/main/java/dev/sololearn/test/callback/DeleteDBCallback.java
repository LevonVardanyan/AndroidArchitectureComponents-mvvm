package dev.sololearn.test.callback;

/**
 * Callback for deleting data from db
 * calls onSuccess() when item deleted, onFailure() otherwise
 */
public interface DeleteDBCallback {
    void onSuccess();
    void onFailure();
}
