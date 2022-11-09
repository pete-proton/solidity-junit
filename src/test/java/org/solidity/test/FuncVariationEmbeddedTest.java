package org.solidity.test;

import com.solidity.test.FuncVariation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.web3j.EVMTest;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EVMTest()
public class FuncVariationEmbeddedTest {
    private final Random random = new Random();

    private FuncVariation deploy(Web3j web3j,
                                 TransactionManager transactionManager,
                                 ContractGasProvider gasProvider) throws Exception {
        return FuncVariation.deploy(web3j, transactionManager, gasProvider).send();
    }

    private BigInteger getCurrent(FuncVariation funcVariation) throws Exception {
        return funcVariation.get().send();
    }

    private <T> void repeat(int times, Callable<T> callable) {
        IntStream.rangeClosed(1, times)
                .forEach(ignore -> {
                    try {
                        callable.call();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Test
    public void initialValue(Web3j web3j,
                             TransactionManager transactionManager,
                             ContractGasProvider gasProvider) throws Exception {

        var funcVariation = deploy(web3j, transactionManager, gasProvider);
        assertEquals(BigInteger.ZERO, getCurrent(funcVariation));
    }

    @Test
    @DisplayName("Increment once and check the value")
    public void inc(Web3j web3j,
                    TransactionManager transactionManager,
                    ContractGasProvider gasProvider) throws Exception {
        var funcVariation = deploy(web3j, transactionManager, gasProvider);

        funcVariation.inc().send();

        var expected = BigInteger.ONE;
        BigInteger actual = getCurrent(funcVariation);

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Increment random number of times and check the value")
    public void incByRandom(Web3j web3j,
                            TransactionManager transactionManager,
                            ContractGasProvider gasProvider) throws Exception {
        var funcVariation = deploy(web3j, transactionManager, gasProvider);

        int increment = random.nextInt(10);

        repeat(increment, () -> funcVariation.inc().send());

        var expected = BigInteger.valueOf(increment);
        BigInteger actual = getCurrent(funcVariation);

        assertEquals(expected, actual, String.format("Incremented %d times. %s != %s", increment, actual, expected));
    }

    @Test
    @DisplayName("Increment twice, decrement once and check the value")
    public void dec(Web3j web3j,
                    TransactionManager transactionManager,
                    ContractGasProvider gasProvider) throws Exception {
        var funcVariation = deploy(web3j, transactionManager, gasProvider);

        funcVariation.inc().send();
        funcVariation.inc().send();

        funcVariation.dec().send();

        var expected = BigInteger.ONE;
        BigInteger actual = getCurrent(funcVariation);

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Increment and decrement random number of times and check the value")
    public void decByRandom(Web3j web3j,
                            TransactionManager transactionManager,
                            ContractGasProvider gasProvider) throws Exception {
        var funcVariation = deploy(web3j, transactionManager, gasProvider);

        int increment = random.nextInt(20);
        repeat(increment, () -> funcVariation.inc().send());

        int decrement = random.nextInt(increment);

        repeat(decrement, () -> funcVariation.dec().send());

        BigInteger actual = getCurrent(funcVariation);
        var expected = BigInteger.valueOf(increment - decrement);

        assertEquals(expected, actual, String.format("Incremented by %d, decremented by %d. %s != %s", increment, decrement, actual, expected));
    }

    @Test
    @DisplayName("Decrement when value is zero and check the failure")
    public void decFailure(Web3j web3j, TransactionManager transactionManager,
                           ContractGasProvider gasProvider) throws Exception {
        var funcVariation = deploy(web3j, transactionManager, gasProvider);
        assertThrows(TransactionException.class, () -> funcVariation.dec().send());
    }

    @Test
    @DisplayName("Increment by a random number and check the value")
    public void incWith(Web3j web3j,
                        TransactionManager transactionManager,
                        ContractGasProvider gasProvider) throws Exception {
        var funcVariation = deploy(web3j, transactionManager, gasProvider);

        var increment = BigInteger.valueOf(random.nextInt(Integer.MAX_VALUE));
        funcVariation.incWith(increment).send();

        BigInteger actual = getCurrent(funcVariation);
        BigInteger expected = increment;

        assertEquals(expected, actual, String.format("Incremented by %s. %s != %s", increment, actual, expected));
    }

    @Test
    @DisplayName("Payme a random number and check the value")
    public void paymeToIncrement(Web3j web3j, TransactionManager transactionManager,
                                 ContractGasProvider gasProvider) throws Exception {
        var funcVariation = deploy(web3j, transactionManager, gasProvider);
        var increment = BigInteger.valueOf(random.nextInt(Integer.MAX_VALUE));

        funcVariation.paymeToIncrement(increment, BigInteger.ZERO).send();

        var actual = getCurrent(funcVariation);
        BigInteger expected = increment;

        assertEquals(expected, actual, String.format("Payed %s. %s != %s", increment, actual, expected));
    }

    @Test
    @DisplayName("Pay exact a random number and check the value")
    public void payExactToIncrement(Web3j web3j, TransactionManager transactionManager,
                                    ContractGasProvider gasProvider) throws Exception {
        var funcVariation = deploy(web3j, transactionManager, gasProvider);

        var increment = BigInteger.valueOf(random.nextInt(Integer.MAX_VALUE));

        funcVariation.payExactToIncrement(increment, new BigInteger("10000000000000000")).send();
        BigInteger actual = funcVariation.get().send();
        BigInteger expected = increment;

        assertEquals(expected, actual, String.format("Payed %s. %s != %s", increment, actual, expected));
    }

    @Test
    @DisplayName("Pay exact a random number with incorrect wei and check the failure")
    public void payExactToIncrementFailure(Web3j web3j, TransactionManager transactionManager,
                                           ContractGasProvider gasProvider) throws Exception {
        var funcVariation = deploy(web3j, transactionManager, gasProvider);

        var increment = BigInteger.valueOf(random.nextInt(Integer.MAX_VALUE));

        var wei = BigInteger.ONE;
        assertThrows(TransactionException.class,
                () -> funcVariation.payExactToIncrement(increment, wei).send());

        BigInteger actual = funcVariation.get().send();
        var expected = BigInteger.ZERO;

        assertEquals(expected, actual, String.format("Payed %s with wei %s. Should stay zero. %s != %s", increment, wei, actual, expected));
    }
}


