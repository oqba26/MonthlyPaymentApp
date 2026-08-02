package com.oqba26.monthlypaymentapp.data.sync

import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import com.oqba26.monthlypaymentapp.data.model.Person
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست‌های [SyncMerger].
 *
 * تمرکز این تست‌ها روی یک چیز است: **هیچ مسیری نباید داده‌ی سینک‌نشده را پاک کند.**
 * این همان باگی است که در نسخهٔ قبلی بی‌صدا پرداخت‌های کاربر را از بین می‌برد.
 */
class SyncMergerTest {

    private fun person(
        id: String,
        name: String = "شخص $id",
        needsSync: Boolean = false
    ) = Person(id = id, name = name, needsSync = needsSync)

    private fun payment(
        id: String,
        personId: String = "p1",
        amount: Double = 1000.0,
        needsSync: Boolean = false
    ) = PaymentRecord(
        id = id,
        personId = personId,
        amount = amount,
        shamsiYear = 1404,
        shamsiMonth = 5,
        needsSync = needsSync
    )

    // ---------------------------------------------------------------- اشخاص

    @Test
    fun `شخص در صف سینک حذف نمی شود حتی اگر در سرور نباشد`() {
        val local = listOf(person("a"), person("b"))
        val server = listOf(person("a"))

        val plan = SyncMerger.planPersons(local, server, pendingIds = setOf("b"))

        assertFalse("شخص b در صف است و نباید حذف شود", plan.deleteIds.contains("b"))
        assertEquals(emptyList<String>(), plan.deleteIds)
    }

    @Test
    fun `شخص با needsSync حذف نمی شود حتی اگر صف خالی باشد`() {
        // سناریوی بازماندهٔ باگ قبلی: صف از دست رفته ولی پرچم روی رکورد هست.
        val local = listOf(person("a"), person("b", needsSync = true))
        val server = listOf(person("a"))

        val plan = SyncMerger.planPersons(local, server, pendingIds = emptySet())

        assertEquals(emptyList<String>(), plan.deleteIds)
    }

    @Test
    fun `داده سرور روی شخص در صف بازنویسی نمی شود`() {
        val local = listOf(person("a", name = "نام محلی جدید", needsSync = true))
        val server = listOf(person("a", name = "نام قدیمی سرور"))

        val plan = SyncMerger.planPersons(local, server, pendingIds = setOf("a"))

        assertTrue("رکورد در صف نباید upsert شود", plan.upserts.isEmpty())
    }

    @Test
    fun `شخص محلی غیر pending که در سرور نیست حذف می شود`() {
        val local = listOf(person("a"), person("b"))
        val server = listOf(person("a"))

        val plan = SyncMerger.planPersons(local, server, pendingIds = emptySet())

        assertEquals(listOf("b"), plan.deleteIds)
    }

    @Test
    fun `سرور خالی هیچ رکورد محافظت شده ای را پاک نمی کند`() {
        val local = listOf(
            person("a", needsSync = true),
            person("b"),
            person("c")
        )

        val plan = SyncMerger.planPersons(local, server = emptyList(), pendingIds = setOf("b"))

        // a با needsSync و b از طریق صف محافظت شده‌اند؛ فقط c حذف می‌شود.
        assertEquals(listOf("c"), plan.deleteIds)
        assertTrue(plan.upserts.isEmpty())
    }

    @Test
    fun `upsert پرچم needsSync را پاک می کند`() {
        val plan = SyncMerger.planPersons(
            local = listOf(person("a", needsSync = true)),
            server = listOf(person("a")),
            pendingIds = emptySet()
        )

        // needsSync محلی محافظت می‌کند، پس upsert خالی است.
        assertTrue(plan.upserts.isEmpty())

        // ولی وقتی محافظتی نباشد، رکورد سرور باید needsSync = false داشته باشد.
        val clean = SyncMerger.planPersons(
            local = listOf(person("a")),
            server = listOf(person("a", needsSync = true)),
            pendingIds = emptySet()
        )
        assertEquals(1, clean.upserts.size)
        assertFalse(clean.upserts.first().needsSync)
    }

    @Test
    fun `شخص سرور بدون شناسه به همنام محلی وصل می شود`() {
        val local = listOf(person("local-id", name = "علی رضایی"))
        val server = listOf(person("", name = "  علی رضایی  "))

        val plan = SyncMerger.planPersons(
            local, server, pendingIds = emptySet(),
            idGenerator = { "شناسه-جدید-نباید-استفاده-شود" }
        )

        assertEquals(listOf("local-id"), plan.upserts.map { it.id })
        assertEquals("علی رضایی", plan.upserts.first().name)
        // چون شناسه به رکورد محلی نگاشت شد، نباید حذفی رخ دهد.
        assertEquals(emptyList<String>(), plan.deleteIds)
    }

    @Test
    fun `شخص سرور بدون شناسه و بدون همنام محلی شناسه جدید می گیرد`() {
        val plan = SyncMerger.planPersons(
            local = emptyList(),
            server = listOf(person("", name = "شخص تازه")),
            pendingIds = emptySet(),
            idGenerator = { "شناسه-تولیدی" }
        )

        assertEquals(listOf("شناسه-تولیدی"), plan.upserts.map { it.id })
    }

    // -------------------------------------------------------------- پرداخت‌ها

    @Test
    fun `پرداخت آفلاین کاربر با merge از بین نمی رود`() {
        // این دقیقاً همان سناریویی است که قبلاً داده را نابود می‌کرد:
        // کاربر آفلاین پرداخت ثبت می‌کند، سرور هنوز خبر ندارد، merge اجرا می‌شود.
        val offlinePayment = payment("جدید-آفلاین", needsSync = true)
        val local = listOf(payment("قدیمی-سینک-شده"), offlinePayment)
        val server = listOf(payment("قدیمی-سینک-شده"))

        val plan = SyncMerger.planPayments(local, server, pendingIds = setOf("جدید-آفلاین"))

        assertEquals(emptyList<String>(), plan.deleteIds)
        assertFalse(plan.upserts.any { it.id == "جدید-آفلاین" })
    }

    @Test
    fun `مبلغ ویرایش شده محلی با داده قدیمی سرور بازنویسی نمی شود`() {
        val local = listOf(payment("x", amount = 500_000.0, needsSync = true))
        val server = listOf(payment("x", amount = 200_000.0))

        val plan = SyncMerger.planPayments(local, server, pendingIds = setOf("x"))

        assertTrue(plan.upserts.isEmpty())
    }

    @Test
    fun `پرداخت حذف شده در سرور از گوشی هم حذف می شود`() {
        val local = listOf(payment("x"), payment("y"))
        val server = listOf(payment("x"))

        val plan = SyncMerger.planPayments(local, server, pendingIds = emptySet())

        assertEquals(listOf("y"), plan.deleteIds)
    }

    @Test
    fun `سرور خالی پرداخت های در صف را نگه می دارد`() {
        val local = listOf(
            payment("pending", needsSync = true),
            payment("synced")
        )

        val plan = SyncMerger.planPayments(local, server = emptyList(), pendingIds = emptySet())

        assertEquals(listOf("synced"), plan.deleteIds)
    }

    @Test
    fun `merge بدون تغییر نقشه خالی می دهد`() {
        val rows = listOf(payment("x"), payment("y"))
        val plan = SyncMerger.planPayments(rows, rows, pendingIds = emptySet())

        assertEquals(emptyList<String>(), plan.deleteIds)
        assertEquals(2, plan.upserts.size) // upsert بی‌ضرر است، حذف نیست
    }
}
