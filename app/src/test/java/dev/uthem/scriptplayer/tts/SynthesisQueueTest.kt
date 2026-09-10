package dev.uthem.scriptplayer.tts

import dev.uthem.scriptplayer.parser.parseScript
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 합성 큐.
 *
 * 가짜 합성기로 돈다 — 실제 `TextToSpeech` 는 기기에만 있고, 여기서 보려는 것은
 * "어떤 문장에 어떤 음성으로 요청이 갔는가" 와 "실패를 어떻게 넘기는가" 다.
 */
class SynthesisQueueTest {

    private class FakeSynthesizer(
        private val failOn: Set<String> = emptySet(),
    ) : Synthesizer {
        val requests = mutableListOf<SynthesisRequest>()

        override suspend fun availableVoices(): List<VoiceInfo> = emptyList()

        override suspend fun synthesize(request: SynthesisRequest): Result<SynthesizedSentence> {
            requests += request
            if (request.text in failOn) {
                return Result.failure(RuntimeException("일부러 실패"))
            }
            return Result.success(
                SynthesizedSentence(audio = File("/tmp/${request.text.hashCode()}.wav"), wordTimings = emptyList()),
            )
        }
    }

    private val defaultVoice = SynthesisRequest(text = "", voiceName = "기본", pitch = 1.0f)

    @Test
    fun `문장 순서대로 하나씩 낸다`() = runTest {
        val script = parseScript("하나. 둘. 셋.")
        val fake = FakeSynthesizer()

        val progress = SynthesisQueue(fake).synthesize(script, emptyMap(), defaultVoice).toList()

        assertEquals(
            listOf(0, 1, 2),
            progress.filterIsInstance<SynthesisProgress.Done>().map { it.index },
        )
        assertEquals(SynthesisProgress.Complete, progress.last())
    }

    @Test
    fun `화자마다 그 화자의 음성으로 요청한다`() = runTest {
        val script = parseScript("진행자: 하나.\n게스트: 둘.\n진행자: 셋.\n게스트: 넷.")
        val host = script.speakers.first { it.label == "진행자" }
        val guest = script.speakers.first { it.label == "게스트" }
        val fake = FakeSynthesizer()

        SynthesisQueue(fake).synthesize(
            script,
            mapOf(
                host.id to SynthesisRequest("", "목소리A", 1.0f),
                guest.id to SynthesisRequest("", "목소리B", 0.88f),
            ),
            defaultVoice,
        ).toList()

        assertEquals(
            listOf("목소리A", "목소리B", "목소리A", "목소리B"),
            fake.requests.map { it.voiceName },
        )
        assertEquals(listOf(1.0f, 0.88f, 1.0f, 0.88f), fake.requests.map { it.pitch })
    }

    @Test
    fun `화자 없는 문장은 기본 음성으로 읽는다`() = runTest {
        val script = parseScript("# 머리글\n\n진행자: 하나.\n게스트: 둘.\n진행자: 셋.\n게스트: 넷.")
        val fake = FakeSynthesizer()

        SynthesisQueue(fake).synthesize(script, emptyMap(), defaultVoice).toList()

        // 머리글은 화자가 없다 — 배정이 비어 있어도 기본 음성으로 요청이 가야 한다
        assertTrue("요청이 없다", fake.requests.isNotEmpty())
        assertEquals("기본", fake.requests.first().voiceName)
    }

    /**
     * 한 문장 때문에 전체를 멈추지 않는다.
     *
     * 대본에 특이한 글자가 섞여 그 문장만 실패할 때, 아무것도 못 듣는 것보다 그 문장만
     * 비는 편이 낫다.
     */
    @Test
    fun `한 문장이 실패해도 나머지를 계속 합성한다`() = runTest {
        val script = parseScript("하나. 둘. 셋.")
        val fake = FakeSynthesizer(failOn = setOf("둘."))

        val progress = SynthesisQueue(fake).synthesize(script, emptyMap(), defaultVoice).toList()

        assertEquals(
            listOf(0, 2),
            progress.filterIsInstance<SynthesisProgress.Done>().map { it.index },
        )
        assertEquals(
            listOf(1),
            progress.filterIsInstance<SynthesisProgress.Failed>().map { it.index },
        )
        assertEquals(SynthesisProgress.Complete, progress.last())
    }

    @Test
    fun `실패한 까닭을 함께 낸다`() = runTest {
        val script = parseScript("하나.")
        val fake = FakeSynthesizer(failOn = setOf("하나."))

        val progress = SynthesisQueue(fake).synthesize(script, emptyMap(), defaultVoice).toList()

        assertEquals(
            "일부러 실패",
            progress.filterIsInstance<SynthesisProgress.Failed>().single().reason,
        )
    }

    @Test
    fun `문장이 없으면 바로 끝난다`() = runTest {
        val script = parseScript("```\ncode\n```")
        val fake = FakeSynthesizer()

        val progress = SynthesisQueue(fake).synthesize(script, emptyMap(), defaultVoice).toList()

        assertEquals(listOf(SynthesisProgress.Complete), progress)
        assertTrue("합성을 부르지 않아야 한다", fake.requests.isEmpty())
    }

    /** 첫 문장이 준비되면 뒤가 다 끝나기 전에 흘러나와야 한다 — 스트리밍 시작의 근거다. */
    @Test
    fun `첫 문장은 전체 합성을 기다리지 않고 나온다`() = runTest {
        val script = parseScript("하나. 둘. 셋. 넷. 다섯.")
        val fake = FakeSynthesizer()

        val flow = SynthesisQueue(fake).synthesize(script, emptyMap(), defaultVoice)
        var seenAfterFirst = 0
        flow.collect { progress ->
            if (progress is SynthesisProgress.Done && progress.index == 0) {
                seenAfterFirst = fake.requests.size
            }
        }

        assertEquals("첫 문장이 나올 때 요청은 한 건이어야 한다", 1, seenAfterFirst)
    }
}
