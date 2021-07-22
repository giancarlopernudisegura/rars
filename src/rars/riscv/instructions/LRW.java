package rars.riscv.instructions;

import rars.Globals;
import rars.riscv.hardware.AddressErrorException;
import rars.ProgramStatement;
import rars.SimulationException;
import rars.riscv.hardware.RegisterFile;
import rars.riscv.hardware.ReservationTable.bitWidth;

public class LRW extends Atomic {
    public LRW() {
        super("lr.w t1, (t2)", "Set t1 to contents of effective memory word address and reserve", "010", "00010");
    }

    public void simulate(ProgramStatement statement) throws SimulationException {
        int[] operands = statement.getOperands();
        try {
            if(statement.getCurrentHart() == -1)
                RegisterFile.updateRegister(operands[0], load(RegisterFile.getValue(operands[1]), statement.getCurrentHart()));
            else
            RegisterFile.updateRegister(operands[0], load(RegisterFile.getValue(operands[1], statement.getCurrentHart()), statement.getCurrentHart()), statement.getCurrentHart());
        } catch (AddressErrorException e) {
            throw new SimulationException(statement, e);
        }
    }

    private long load(int address, int hart) throws AddressErrorException {
        Globals.reservationTables.reserveAddress(hart + 1, address, bitWidth.word);
        return Globals.memory.getWord(address);
    }
}
