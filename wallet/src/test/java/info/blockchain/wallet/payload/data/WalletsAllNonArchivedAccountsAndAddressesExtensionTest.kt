package info.blockchain.wallet.payload.data

import org.amshove.kluent.`should equal`
import org.junit.Test

class WalletsAllNonArchivedAccountsAndAddressesExtensionTest {

    private fun importedAddressWithPrivateKey(address: String) =
        ImportedAddress().also {
            it.privateKey = "PRIVATE_KEY"
            it.address = address
        }

    @Test
    fun `empty list`() {
        Wallet().allNonArchivedAccountsAndAddresses() `should equal` emptySet()
    }

    @Test
    fun `one spendable`() {
        Wallet().apply {
            importedAddressList.add(importedAddressWithPrivateKey("Address1"))
        }.allNonArchivedAccountsAndAddresses() `should equal` setOf("Address1")
    }

    @Test
    fun `one archived`() {
        Wallet().apply {
            importedAddressList.add(importedAddressWithPrivateKey("Address1").apply { archive() })
        }.allNonArchivedAccountsAndAddresses() `should equal` emptySet()
    }

    @Test
    fun `one without private key`() {
        Wallet().apply {
            importedAddressList.add(ImportedAddress().apply {
                address = "Address1"
            })
        }.allNonArchivedAccountsAndAddresses() `should equal` setOf("Address1")
    }

    @Test
    fun `two spendable`() {
        Wallet().apply {
            importedAddressList.add(importedAddressWithPrivateKey("Address1"))
            importedAddressList.add(importedAddressWithPrivateKey("Address2"))
        }.allNonArchivedAccountsAndAddresses() `should equal` setOf("Address1", "Address2")
    }

    @Test
    fun `repeated address`() {
        Wallet().apply {
            importedAddressList.add(importedAddressWithPrivateKey("Address1"))
            importedAddressList.add(importedAddressWithPrivateKey("Address1"))
        }.allNonArchivedAccountsAndAddresses() `should equal` setOf("Address1")
    }

    @Test
    fun `one xpub`() {
        Wallet().apply {
            hdWallets = listOf(hdWallet("XPub1"))
        }.allNonArchivedAccountsAndAddresses() `should equal` setOf("XPub1")
    }

    @Test
    fun `two xpubs`() {
        Wallet().apply {
            hdWallets = listOf(hdWallet("XPub1", "XPub2"))
        }.allNonArchivedAccountsAndAddresses() `should equal` setOf("XPub1", "XPub2")
    }

    @Test
    fun `repeated xpubs`() {
        Wallet().apply {
            hdWallets = listOf(hdWallet("XPub1", "XPub1"))
        }.allNonArchivedAccountsAndAddresses() `should equal` setOf("XPub1")
    }

    @Test
    fun `two xpubs, two spendable address and two non-spendable`() {
        Wallet().apply {
            hdWallets = listOf(hdWallet("XPub1", "XPub2"))
            importedAddressList.add(importedAddressWithPrivateKey("Address1"))
            importedAddressList.add(importedAddressWithPrivateKey("Address2"))
            importedAddressList.add(
                ImportedAddress().also { it.address = "Address3" })
            importedAddressList.add(importedAddressWithPrivateKey("Address4").apply { archive() })
        }.allNonArchivedAccountsAndAddresses() `should equal` setOf(
            "XPub1",
            "XPub2",
            "Address1",
            "Address2",
            "Address3"
        )
    }

    private fun hdWallet(vararg xpubs: String) =
        HDWallet().apply {
            accounts = xpubs.map {
                Account().apply {
                    xpub = it
                }
            }
        }
}
