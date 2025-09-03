package canaryprism.mcwm.swing.nbt;

public enum NBTNodeIcon {
    BYTE_ARRAY_ICON("byte_array.png"),
    BYTE_ICON("byte.png"),
    COMPOUND_ICON("compound.png"),
    DOUBLE_ICON("double.png"),
    FLOAT_ICON("float.png"),
    INT_ARRAY_ICON("int_array.png"),
    INT_ICON("int.png"),
    LIST_ICON("list.png"),
    LIST_CONTAINER_ICON("list_container.png"),
    LONG_ARRAY_ICON("long_array.png"),
    LONG_ICON("long.png"),
    SHORT_ICON("short.png"),
    STRING_ICON("string.png"),
    END_ICON("end.png"),
    TAG_ICON("tag.png"),
    NAMED_TAG_ICON("named.png");

    public final String file_name;
    NBTNodeIcon(String file_name) {
        this.file_name = file_name;
    }
}
