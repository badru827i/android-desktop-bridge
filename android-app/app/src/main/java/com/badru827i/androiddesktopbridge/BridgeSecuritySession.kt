package com.badru827i.androiddesktopbridge

import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

/** Pairing/session state. Transport code must reject blocked peers and stale sequence numbers. */
class BridgeSecuritySession {
    private val random = SecureRandom()
    private val blockedPeers = ConcurrentHashMap.newKeySet<String>()
    private var activePeer: String? = null
    private var lastSequence: Long = -1

    fun newPairingCode(): String = (100000 + random.nextInt(900000)).toString()

    @Synchronized fun beginSession(peerId: String) {
        require(peerId !in blockedPeers) { "Peer is blocked" }
        activePeer = peerId
        lastSequence = -1
    }

    @Synchronized fun validateSequence(peerId: String, sequence: Long): Boolean {
        if (peerId != activePeer || peerId in blockedPeers || sequence <= lastSequence) return false
        lastSequence = sequence
        return true
    }

    @Synchronized fun rejectPeer(peerId: String) {
        blockedPeers += peerId
        if (activePeer == peerId) clearSession()
    }

    @Synchronized fun clearSession() {
        activePeer = null
        lastSequence = -1
    }

    fun isBlocked(peerId: String): Boolean = peerId in blockedPeers
}
