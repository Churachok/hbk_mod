package dev.kirill.hbk.item;

/** The original white-and-magenta member weapon. */
public class AttackingMemberItem extends MemberWeaponItem {
	public static final float BULLET_DAMAGE = 10.0f;

	public AttackingMemberItem(Properties properties) {
		super(properties, BULLET_DAMAGE);
	}
}
