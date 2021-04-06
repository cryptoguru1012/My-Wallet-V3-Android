package info.blockchain.wallet.payload.data

import org.amshove.kluent.`should equal`
import org.junit.Test

class WalletsAllSpendableAccountsAndAddressesExtensionTest {

    private fun importedAddressWithPrivateKey(address: String) =
        ImportedAddress().also {
            it.privateKey = "PRIVATE_KEY"
            it.address = address
        }

    @Test
    fun `empty list`() {
        Wallet().allSpendableAccountsAndAddresses() `should equal` emptySet()
    }

    @Test
    fun `one spendable`() {
        Wallet().apply {
            importedAddressList.add(importedAddressWithPrivateKey("Address1"))
        }.allSpendableAccountsAndAddresses() `should equal` setOf("Address1")
    }

    @Test
    fun `one archived`() {
        Wallet().apply {
            importedAddressList.add(importedAddressWithPrivateKey("Address1").apply { archive() })
        }.allSpendableAccountsAndAddresses() `should equal` emptySet()
    }

    @Test
    fun `two spendable`() {
        Wallet().apply {
            importedAddressList.add(importedAddressWithPrivateKey("Address1"))
            importedAddressList.add(importedAddressWithPrivateKey("Address2"))
        }.allSpendableAccountsAndAddresses() `should equal` setOf("Address1", "Address2")
    }

    @Test
    fun `repeated address`() {
        Wallet().apply {
            importedAddressList.add(importedAddressWithPrivateKey("Address1"))
            importedAddressList.add(importedAddressWithPrivateKey("Address1"))
        }.allSpendableAccountsAndAddresses() `should equal` setOf("Address1")
    }

    @Test
    fun `one xpub`() {
        Wallet().apply {
            hdWallets = listOf(hdWallet("XPub1"))
        }.allSpendableAccountsAndAddresses() `should equal` setOf("XPub1")
    }

    @Test
    fun `two xpubs`() {
        Wallet().apply {
            hdWallets = listOf(hdWallet("XPub1", "XPub2"))
        }.allSpendableAccountsAndAddresses() `should equal` setOf("XPub1", "XPub2")
    }

    @Test
    fun `repeated xpubs`() {
        Wallet().apply {
            hdWallets = listOf(hdWallet("XPub1", "XPub1"))
        }.allSpendableAccountsAndAddresses() `should equal` setOf("XPub1")
    }

    @Test
    fun `two xpubs, two spendable address and two non-spendable`() {
        Wallet().apply {
            hdWallets = listOf(hdWallet("XPub1", "XPub2"))
            importedAddressList.add(importedAddressWithPrivateKey("Address1"))
            importedAddressList.add(importedAddressWithPrivateKey("Address2"))
            importedAddressList.add(importedAddressWithPrivateKey("Address4").apply { archive() })
        }.allSpendableAccountsAndAddresses() `should equal` setOf("XPub1", "XPub2", "Address1", "Address2")
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
