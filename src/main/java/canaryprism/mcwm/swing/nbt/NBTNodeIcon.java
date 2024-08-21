package canaryprism.mcwm.swing.nbt;

public enum NBTNodeIcon {
    byte_array_icon("byte_array.png"),
    byte_icon("byte.png"),
    compound_icon("compound.png"),
    double_icon("double.png"),
    float_icon("float.png"),
    int_array_icon("int_array.png"),
    int_icon("int.png"),
    list_icon("list.png"),
    list_container_icon("list_container.png"),
    long_array_icon("long_array.png"),
    long_icon("long.png"),
    short_icon("short.png"),
    string_icon("string.png"),
    tag_icon("tag.png"),
    named_tag_icon("named.png");

    public final String file_name;
    NBTNodeIcon(String file_name) {
        this.file_name = file_name;
    }
}
