package dev.kirill.hbk.item;

/** Orange-and-blue sword that blocks like a shield on RMB. */
public final class ArmoredMemberItem extends DestructiveMemberItem {
	public ArmoredMemberItem(Properties properties) {
		super(properties, Ability.SIEGE);
	}
}
