package fr.ensimag.deca.tree;

import java.io.PrintStream;

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
import fr.ensimag.ima.pseudocode.instructions.FLOAT;
import fr.ensimag.ima.pseudocode.instructions.INT;
import fr.ensimag.ima.pseudocode.instructions.LEA;
import fr.ensimag.ima.pseudocode.instructions.LOAD;

/**
 * <p>Cast class.</p>
 *
 * @author oscarmaggiori
 * @version $Id: $Id
 */
public class Cast extends AbstractExpr {
    private AbstractIdentifier type;
    private AbstractExpr e;

    /**
     * <p>Constructor for Cast.</p>
     *
     * @param type a {@link fr.ensimag.deca.tree.AbstractIdentifier} object
     * @param e a {@link fr.ensimag.deca.tree.AbstractExpr} object
     */
    public Cast (AbstractIdentifier type, AbstractExpr e){
        this.type = type;
        this.e = e;
    }

    /** {@inheritDoc} */
    @Override
    public Type verifyExpr(DecacCompiler compiler, EnvironmentExp localEnv, ClassDefinition currentClass)
            throws ContextualError {

        if (e == null) {
            throw new ContextualError("Expression is null", getLocation());
        }
        if (type == null) {
            throw new ContextualError("Type is null", getLocation());
        }
        Type type = this.type.verifyType(compiler);
        Type expressionType = this.e.verifyExpr(compiler, localEnv, currentClass);
        if(expressionType.isVoid()){
            throw new ContextualError("cannot cast void type", getLocation());
        }
        if(expressionType.isNull()){
            throw new ContextualError("cannot cast null type", getLocation());
        }
        if(compiler.assignCompatible(compiler, type, expressionType)!= null){
            this.setType(type);
            return type;
        }
        if(compiler.assignCompatible(compiler, expressionType, type)!= null){
            this.setType(type);
            return type;
        }
        throw new ContextualError("impossible cast", getLocation());
    }

    /** {@inheritDoc} */
    @Override
    public void decompile(IndentPrintStream s) {
        s.print("(");
        type.decompile(s);
        s.print(") (");
        e.decompile(s);
        s.print(")");        
    }

    /** {@inheritDoc} */
    @Override
    protected void prettyPrintChildren(PrintStream s, String prefix) {
        type.prettyPrint(s, prefix, false);
        e.prettyPrint(s, prefix, false);
        
    }

    /** {@inheritDoc} */
    @Override
    protected void iterChildren(TreeFunction f) {
        type.iter(f);
        e.iter(f);
    }

    private void isInstanceOf(DecacCompiler compiler, Label doCast) {
        e.codeGenInst(compiler);
        // exprAddrReg contient l'adresse de e dans le tas
        GPRegister exprAddrReg = compiler.getData().getLastUsedRegister(); 
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
        compiler.addInstruction(new BEQ(doCast));
        compiler.addLabel(W_Cond);
        // Tester si la classe parent de e est nulle
        compiler.addInstruction(new CMP(new NullOperand(), reg));
        compiler.addInstruction(new BNE(W_Start));
    }

    /** {@inheritDoc} */
    @Override
    protected void codeGenInst(DecacCompiler compiler) {
        e.codeGenInst(compiler);
        GPRegister lastUsed = compiler.getData().getLastUsedRegister();
        if ((type.getType().isInt()) && (e.getType().isFloat())) {
            compiler.addInstruction(new INT(lastUsed, lastUsed));
        } else if ((type.getType().isFloat()) && (e.getType().isInt())) {
            compiler.addInstruction(new FLOAT(lastUsed, lastUsed));
        } else if (type.getType().isClass()) {
            if (e.getType().isNull()) {
            } else {
                Label doCast = new Label("do_cast."+compiler.getNLabel());
                compiler.incrNLabel();
                // Tester en assembleur si e est une instance de type
                isInstanceOf(compiler, doCast);
                // On cast
                if (!(compiler.getCompilerOptions().getNoCheck())) {
                    compiler.addInstruction(new BRA(new Label("cast_error")));
                }
                compiler.addLabel(doCast);
            }
        } else {
            if (!(compiler.getCompilerOptions().getNoCheck())) { 
                compiler.addInstruction(new BRA(new Label("cast_error")));
            }
        }
    }
}
