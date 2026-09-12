package earth.terrarium.argonauts.api.teams.guild;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.bytecodecs.defaults.MapCodec;
import com.teamresourceful.resourcefullib.common.utils.TriState;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Objects;

/**
 * A guild role. Roles hold tristate permission overrides and can inherit from a parent role.
 *
 * @param id        The role id
 * @param parent    The parent role id, or an empty string if the role has no parent
 * @param overrides A map of permission/setting ids to their tristate overrides
 */
public class Role {

    public static final String ALL = "all";
    public static final String MEMBER = "member";
    public static final String ALLY = "ally";

    public static final ByteCodec<Role> BYTE_CODEC = ObjectByteCodec.create(
        ByteCodec.STRING.fieldOf(Role::id),
        ByteCodec.STRING.fieldOf(Role::parent),
        new MapCodec<>(ByteCodec.STRING, ByteCodec.ofEnum(TriState.class))
            .map(map -> (Object2ObjectMap<String, TriState>) new Object2ObjectOpenHashMap<>(map), map -> map
            ).fieldOf(Role::overrides),
        Role::new
    );

    private final String id;
    private String parent;
    private final Object2ObjectMap<String, TriState> overrides;

    public Role(String id, String parent, Object2ObjectMap<String, TriState> overrides) {
        this.id = id;
        this.parent = parent;
        this.overrides = new Object2ObjectOpenHashMap<>(overrides);
    }

    public Role(String id, String parent) {
        this(id, parent, new Object2ObjectOpenHashMap<>());
    }

    public String id() {
        return this.id;
    }

    public String parent() {
        return this.parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public boolean hasParent() {
        return !this.parent.isEmpty();
    }

    public Object2ObjectMap<String, TriState> overrides() {
        return this.overrides;
    }

    public TriState override(String id) {
        return this.overrides.getOrDefault(id, TriState.UNDEFINED);
    }

    public void setOverride(String id, TriState state) {
        if (state == TriState.UNDEFINED) {
            this.overrides.remove(id);
        } else {
            this.overrides.put(id, state);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Role) obj;
        return Objects.equals(this.id, that.id) &&
            Objects.equals(this.parent, that.parent) &&
            Objects.equals(this.overrides, that.overrides);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, parent, overrides);
    }

    @Override
    public String toString() {
        return "Role[" +
            "id=" + id + ", " +
            "parent=" + parent + ", " +
            "overrides=" + overrides + ']';
    }
}
