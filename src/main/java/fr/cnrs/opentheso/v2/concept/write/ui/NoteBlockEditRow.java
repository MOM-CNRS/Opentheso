package fr.cnrs.opentheso.v2.concept.write.ui;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class NoteBlockEditRow implements Serializable {

    private int noteId;
    private String typeCode;
    private String lang;
    private String value;
    private String source;
    private boolean existing;

    public NoteBlockEditRow(
            int noteId,
            String typeCode,
            String lang,
            String value,
            String source,
            boolean existing
    ) {
        this.noteId = noteId;
        this.typeCode = typeCode;
        this.lang = lang;
        this.value = value;
        this.source = source;
        this.existing = existing;
    }
}
