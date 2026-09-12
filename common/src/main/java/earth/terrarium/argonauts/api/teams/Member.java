package earth.terrarium.argonauts.api.teams;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.bytecodecs.defaults.MapCodec;
import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.argonauts.api.teams.guild.Role;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Objects;

public class Member {

    public static final ByteCodec<Member> BYTE_CODEC = ObjectByteCodec.create(
        MemberStatus.BYTE_CODEC.fieldOf(Member::status),
        new MapCodec<>(ByteCodec.STRING, ByteCodec.BOOLEAN)
            .map(map -> (Object2BooleanMap<String>) new Object2BooleanOpenHashMap<>(map), map -> map
            ).fieldOf(Member::permissions),
        ByteCodec.STRING.fieldOf(Member::name),
        ByteCodec.STRING.fieldOf(Member::role),
        new MapCodec<>(ByteCodec.STRING, ByteCodec.ofEnum(TriState.class))
            .map(map -> (Object2ObjectMap<String, TriState>) new Object2ObjectOpenHashMap<>(map), map -> map
            ).fieldOf(Member::permissionOverrides),
        Member::new
    );

    private MemberStatus status;
    private final Object2BooleanMap<String> permissions;
    private String name;
    private String role;
    private final Object2ObjectMap<String, TriState> permissionOverrides;

    public Member(MemberStatus status, Object2BooleanMap<String> permissions) {
        this(status, permissions, "");
    }

    public Member(MemberStatus status, Object2BooleanMap<String> permissions, String name) {
        this(status, permissions, name, Role.MEMBER, new Object2ObjectOpenHashMap<>());
    }

    public Member(MemberStatus status, Object2BooleanMap<String> permissions, String name, String role, Object2ObjectMap<String, TriState> permissionOverrides) {
        this.status = status;
        this.permissions = new Object2BooleanOpenHashMap<>(permissions);
        this.name = name;
        this.role = role;
        this.permissionOverrides = new Object2ObjectOpenHashMap<>(permissionOverrides);
    }

    public boolean isOwner() {
        return this.status.isOwner();
    }

    public MemberStatus status() {
        return this.status;
    }

    public void setStatus(MemberStatus status) {
        this.status = status;
    }

    public String name() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String role() {
        return this.role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Object2BooleanMap<String> permissions() {
        return this.permissions;
    }

    public boolean hasPermission(String permission) {
        return this.permissions.getBoolean(permission);
    }

    public void setPermission(String permission, boolean value) {
        this.permissions.put(permission, value);
        this.setPermissionOverride(permission, value ? TriState.TRUE : TriState.FALSE);
    }

    public Object2ObjectMap<String, TriState> permissionOverrides() {
        return this.permissionOverrides;
    }

    public TriState permissionOverride(String permission) {
        return this.permissionOverrides.getOrDefault(permission, TriState.UNDEFINED);
    }

    public void setPermissionOverride(String permission, TriState state) {
        if (state == TriState.UNDEFINED) {
            this.permissionOverrides.remove(permission);
        } else {
            this.permissionOverrides.put(permission, state);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Member) obj;
        return Objects.equals(this.status, that.status) &&
            Objects.equals(this.permissions, that.permissions) &&
            Objects.equals(this.name, that.name) &&
            Objects.equals(this.role, that.role) &&
            Objects.equals(this.permissionOverrides, that.permissionOverrides);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, permissions, name, role, permissionOverrides);
    }

    @Override
    public String toString() {
        return "Member[" +
            "status=" + status + ", " +
            "permissions=" + permissions + ", " +
            "name=" + name + ", " +
            "role=" + role + ", " +
            "permissionOverrides=" + permissionOverrides + ']';
    }
}
