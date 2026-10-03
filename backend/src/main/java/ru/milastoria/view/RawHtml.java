package ru.milastoria.view;

import gg.jte.Content;
import gg.jte.TemplateOutput;

/** Неэкранированный HTML (слоганы, блок «О нас»). */
public final class RawHtml implements Content {

    private final String html;

    public RawHtml(String html) {
        this.html = html == null ? "" : html;
    }

    public static RawHtml of(String html) {
        return new RawHtml(html);
    }

    @Override
    public void writeTo(TemplateOutput output) {
        output.writeUnsafeContent(html);
    }
}
