package dev.sololearn.test.util;

import dev.sololearn.test.datamodel.local.Article;

public class PinUnPinPendingEvent {
    public static final int MAKE_PIN = 1;
    public static final int MAKE_UN_PIN = 2;
    Article article;
    int action;

    public PinUnPinPendingEvent(Article article, int action) {
        this.article = article;
        this.action = action;
    }

    public Article getArticle() {
        return article;
    }

    public int getAction() {
        return action;
    }
}
