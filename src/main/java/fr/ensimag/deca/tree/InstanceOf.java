package fr.ensimag.deca.tree;

import java.io.PrintStream;

import org.apache.commons.lang.Validate;

import fr.ensimag.deca.DecacCompiler;
import fr.ensimag.deca.context.ClassDefinition;
import fr.ensimag.deca.context.ClassType;
import fr.ensimag.deca.context.ContextualError;
import fr.ensimag.deca.context.EnvironmentExp;
import fr.ensimag.deca.context.Type;
import fr.ensimag.deca.tools.IndentPrintStream;
import fr.ensimag.ima.pseudocode.GPRegister;
import fr.ensimag.ima.pseudocode.Label;
import fr.ensimag.ima.pseudocode.NullOperand;
import fr.ensimag.ima.pseudocode.Register;
import fr.ensimag.ima.pseudocode.RegisterOffset;
import fr.ensimag.ima.pseudocode.instructions.BEQ;
import fr.ensimag.ima.pseudocode.instructions.BNE;
import fr.ensimag.ima.pseudocode.instructions.BRA;
import fr.ensimag.ima.pseudocode.instructions.CMP;
import fr.ensimag.ima.pseudocode.instructions.LEA;
import fr.ensimag.ima.pseudocode.instructions.LOAD;

/**
 * Operator "x instanceof y"
 *
 * @author oscarmaggiori
 * @version $Id: $Id
 */
public class InstanceOf extends AbstractExpr {

    private AbstractExpr e;
    private AbstractIdentifier type;

    /**
     * <p>Constructor for InstanceOf.</p>
     *
     * @param e a {@link fr.ensimag.deca.tree.AbstractExpr} object
     * @param type a {@link fr.ensimag.deca.tree.AbstractIdentifier} object
     */
    public InstanceOf(AbstractExpr e, AbstractIdentifier type) {
        super();
        Validate.notNull(e);
        Validate.notNull(type);
        this.type = type;
        this.e = e;
    }

    /** {@inheritDoc} */
    @Override
    public Type verifyExpr(DecacCompiler compiler, EnvironmentExp localEnv, ClassDefinition currentClass)
            throws ContextualError {
        Type type1 = this.e.verifyExpr(compiler, localEnv, currentClass);
        Type type2 = this.type.verifyType(compiler);
        if ((type1.isNull() || type1.isClass()) && type2.isClass()) {
            Type newtype = compiler.searchSymbol(compiler.getSymbolTable().create("boolean"));
            this.setType(newtype);
            return newtype;
        }
        throw new ContextualError("Incorrect types", getLocation());
    }

    /** {@inheritDoc} */
    @Override
    public void decompile(IndentPrintStream s) {
        e.decompile(s);
        s.print(" instanceof ");
        type.decompile(s);
    }

    /** {@inheritDoc} */
    @Override
    protected void prettyPrintChildren(PrintStream s, String prefix) {
        e.prettyPrint(s, prefix, false);
        type.prettyPrint(s, prefix, true);
    }

    /** {@inheritDoc} */
    @Override
    protected void iterChildren(TreeFunction f) {
        e.iter(f);
        type.iter(f);

    }

    private void isInstanceOf(DecacCompiler compiler) {
        e.codeGenInst(compiler);
        // exprAddrReg contient l'adresse de e dans le tas
        GPRegister exprAddrReg = compiler.getData().getLastUsedRegister(); 
        Label true_instanceof = new Label("true.instanceof."+compiler.getNLabel());
        Label W_Start = new Label("W.Start"+compiler.getNLabel());
        Label W_Cond = new Label("W.Cond."+compiler.getNLabel());
        ClassType cType = (ClassType)type.getType();
        ClassDefinition typeCible = cType.getDefinition();
        GPRegister reg = compiler.getData().getFreeRegister(compiler);
        compiler.addInstruction(new LOAD(exprAddrReg, reg));
        compiler.addInstruction(new BRA(W_Cond));
        compiler.addLabel(W_Start);
        // Comparer les adresses, sauter à true_instanceof si ok,
        // sinon chercher la classe parent de expr
        compiler.addInstruction(new LEA(typeCible.getAddressVTable(), Register.R0));
        compiler.addInstruction(new LOAD(new RegisterOffset(0, reg), reg));
        compiler.addInstruction(new CMP(Register.R0, reg));
        compiler.addInstruction(new BEQ(true_instanceof));
        compiler.addLabel(W_Cond);
        // Tester si la classe parent de e est nulle
        compiler.addInstruction(new CMP(new NullOperand(), reg));
        compiler.addInstruction(new BNE(W_Start));
    }

    /** {@inheritDoc} */
    @Override
    protected void codeBoolean(boolean b, Label E, DecacCompiler compiler) {
        Label true_instanceof = new Label("true.instanceof."+compiler.getNLabel());
        isInstanceOf(compiler);
        if (!b) {
            compiler.addInstruction(new BRA(E));
        }
        compiler.addLabel(true_instanceof);
        compiler.incrNLabel();
    }

    /** {@inheritDoc} */
    @Override
    protected void codeGenInst(DecacCompiler compiler) {
        Label true_instanceof = new Label("true.instanceof."+compiler.getNLabel());
        Label end_instanceof = new Label("end.instanceof."+compiler.getNLabel());
        isInstanceOf(compiler);
        GPRegister resultRegister = compiler.getData().getFreeRegister(compiler);
        compiler.addInstruction(new LOAD(0, resultRegister));
        compiler.addInstruction(new BRA(end_instanceof));
        compiler.addLabel(true_instanceof);
        compiler.addInstruction(new LOAD(1, resultRegister));
        compiler.getData().setLastUsedRegister(resultRegister);
        compiler.addLabel(end_instanceof);
        compiler.incrNLabel();
    }

}
