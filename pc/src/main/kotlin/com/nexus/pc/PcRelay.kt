package com.nexus.pc

/**
 * Transport between phone and PC. Phase 5 defines the contract only.
 * A relay implementation (Supabase function or local network) is not written yet,
 * so pairing cannot complete end to end until one exists.
 */
interface PcRelay {
    /** Publishes the offer so a companion that knows the code can claim it. */
    suspend fun publishOffer(offer: PairingOffer): Result<Unit>
    /** Companion pushes finished sessions; the phone calls PcSessionSync.ingest with them. */
    suspend fun pullPending(deviceId: String): Result<List<IncomingPcSession>>
}
