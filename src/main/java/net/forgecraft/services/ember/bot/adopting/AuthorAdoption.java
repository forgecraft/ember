package net.forgecraft.services.ember.bot.adopting;

/**
 * The concept of author adoption is for a discord user to assign a modrinth or curseforge account to their discord account.
 * The system works on self identification and has no verification process. The only limitations is that the user can only
 * own one normal account and many organization accounts.
 * <p>
 * Curseforge does not support the concept of organizations so any additional adoptions with be considered organizations.
 *
 * @implNote
 * Modrinth:
 * - Valid id's: fRiHVvU7
 * - Valid slugs: emi-team
 * Curseforge:
 * - Valid url https://www.curseforge.com/members/flanks255/
 * - Valid slug: flanks255
 * - Valid id: 19768394 // Not sure if there is an api for this
 */
public class AuthorAdoption {

}
