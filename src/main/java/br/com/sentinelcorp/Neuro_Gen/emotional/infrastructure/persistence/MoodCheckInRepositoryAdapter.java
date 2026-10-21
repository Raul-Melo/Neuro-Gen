package br.com.sentinelcorp.Neuro_Gen.emotional.infrastructure.persistence;

import br.com.sentinelcorp.Neuro_Gen.emotional.domain.model.EnergyLevel;
import br.com.sentinelcorp.Neuro_Gen.emotional.domain.model.MoodCheckIn;
import br.com.sentinelcorp.Neuro_Gen.emotional.domain.model.MoodCheckInId;
import br.com.sentinelcorp.Neuro_Gen.emotional.domain.model.MoodScore;
import br.com.sentinelcorp.Neuro_Gen.emotional.domain.port.MoodCheckInRepository;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.AuditMetadata;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import br.com.sentinelcorp.Neuro_Gen.shared.security.AesGcmCipher;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Adaptador do check-in de humor. Cifra/decifra {@code notes} (dado sensível) com AES-256-GCM. */
@Repository
public class MoodCheckInRepositoryAdapter implements MoodCheckInRepository {

    private final MoodCheckInJpaRepository jpa;
    private final AesGcmCipher cipher;

    public MoodCheckInRepositoryAdapter(MoodCheckInJpaRepository jpa, AesGcmCipher cipher) {
        this.jpa = jpa;
        this.cipher = cipher;
    }

    @Override
    public MoodCheckIn save(MoodCheckIn c) {
        MoodCheckInEntity e = jpa.findById(c.id().value()).orElseGet(MoodCheckInEntity::new);
        e.setId(c.id().value());
        e.setUserId(c.userId().value());
        e.setMoodScore((short) c.moodScore().value());
        e.setEnergyLevel((short) c.energyLevel().value());
        e.setNotesEnc(encrypt(c.notes()));
        e.setCheckinDate(c.checkInDate());
        e.setCheckinTime(c.checkInTime());
        if (e.getCreatedAt() == null) {
            e.setCreatedAt(c.audit().createdAt());
        }
        jpa.save(e);
        return c;
    }

    @Override
    public Optional<MoodCheckIn> findById(MoodCheckInId id) {
        return jpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<MoodCheckIn> findByUserAndPeriod(UserId userId, LocalDate from, LocalDate to) {
        return jpa.findByUserIdAndCheckinDateBetweenOrderByCheckinDateAscCheckinTimeAsc(
                        userId.value(), from, to)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<MoodCheckIn> findTodayByUserId(UserId userId) {
        return jpa.findFirstByUserIdAndCheckinDate(userId.value(), LocalDate.now()).map(this::toDomain);
    }

    private MoodCheckIn toDomain(MoodCheckInEntity e) {
        return MoodCheckIn.reconstitute(
                new MoodCheckInId(e.getId()),
                new UserId(e.getUserId()),
                new MoodScore(e.getMoodScore()),
                new EnergyLevel(e.getEnergyLevel()),
                e.getCheckinDate(),
                e.getCheckinTime(),
                decrypt(e.getNotesEnc()),
                new AuditMetadata(e.getCreatedAt(), e.getCreatedAt(), e.getUserId().toString()));
    }

    private byte[] encrypt(String plain) {
        if (plain == null) {
            return null;
        }
        return cipher.encryptToBase64(plain).getBytes(StandardCharsets.UTF_8);
    }

    private String decrypt(byte[] enc) {
        if (enc == null) {
            return null;
        }
        return cipher.decryptFromBase64(new String(enc, StandardCharsets.UTF_8));
    }
}
