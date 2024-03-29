package fr.ensimag.deca.tree;

import java.io.PrintStream;
import fr.ensimag.deca.DecacCompiler;
import fr.ensimag.deca.codegen.Data;
import fr.ensimag.deca.context.ClassDefinition;
import fr.ensimag.deca.context.ClassType;
import fr.ensimag.deca.context.ContextualError;
import fr.ensimag.deca.context.EnvironmentExp;
import fr.ensimag.deca.context.MethodDefinition;
import fr.ensimag.deca.context.Type;
import fr.ensimag.deca.tools.IndentPrintStream;
import fr.ensimag.ima.pseudocode.GPRegister;
import fr.ensimag.ima.pseudocode.Label;
import fr.ensimag.ima.pseudocode.NullOperand;
import fr.ensimag.ima.pseudocode.Register;
import fr.ensimag.ima.pseudocode.RegisterOffset;
import fr.ensimag.ima.pseudocode.instructions.ADDSP;
import fr.ensimag.ima.pseudocode.instructions.BEQ;
import fr.ensimag.ima.pseudocode.instructions.BSR;
import fr.ensimag.ima.pseudocode.instructions.CMP;
import fr.ensimag.ima.pseudocode.instructions.LOAD;
import fr.ensimag.ima.pseudocode.instructions.STORE;
import fr.ensimag.ima.pseudocode.instructions.SUBSP;

/**
 * Call of a method
 *
 * @author oscarmaggiori
 * @version $Id: $Id
 */
public class MethodCall extends AbstractExpr {

    private AbstractExpr obj;
    private AbstractIdentifier meth;
    private ListExpr param;

    /**
     * <p>Constructor for MethodCall.</p>
     *
     * @param expr a {@link fr.ensimag.deca.tree.AbstractExpr} object
     * @param method a {@link fr.ensimag.deca.tree.AbstractIdentifier} object
     * @param listExpr a {@link fr.ensimag.deca.tree.ListExpr} object
     */
    public MethodCall(AbstractExpr expr, AbstractIdentifier method, ListExpr listExpr){
        this.obj = expr;
        this.meth = method;
        this.param = listExpr;
    }

    /** {@inheritDoc} */
    @Override
    public Type verifyExpr(DecacCompiler compiler, EnvironmentExp localEnv, ClassDefinition currentClass)
            throws ContextualError {
        Type type = this.obj.verifyExpr(compiler, localEnv, currentClass);
        if (!type.isClass()) {
            throw new ContextualError("object isn't a class", this.getLocation());
        }
        ClassType classType = (ClassType) type ;
        ClassDefinition classDef = classType.getDefinition();
        EnvironmentExp env = classDef.getMembers();
        MethodDefinition methodDef = (MethodDefinition) env.getDictionnary().get(this.meth.getName());
        if (methodDef == null) {
            throw new ContextualError(this.meth.getName()+" not defined", this.getLocation());
        }
        this.meth.setType(methodDef.getType());
        this.meth.setDefinition(methodDef);
        this.setType(methodDef.getType());
        try {
            this.param.verifyListExp(compiler, localEnv, currentClass, methodDef.getSignature());
        } catch (ContextualError e) {
            String message = e.getMessage();
            throw new ContextualError(message, this.getLocation());
        }
        return methodDef.getType();
    }

    /** {@inheritDoc} */
    @Override
    public void decompile(IndentPrintStream s) {
        obj.decompile(s);
        s.print(".");
        meth.decompile(s);
        s.print("(");
        param.decompile(s);
        s.print(")");
    }

    /** {@inheritDoc} */
    @Override
    protected void prettyPrintChildren(PrintStream s, String prefix) {
        obj.prettyPrint(s, prefix,false);
        meth.prettyPrint(s, prefix, false);
        param.prettyPrint(s, prefix, true);
    }

    /** {@inheritDoc} */
    @Override
    protected void iterChildren(TreeFunction f) {
        obj.iter(f);
        meth.iter(f);
        for (AbstractExpr i : param.getList()) {
            i.iter(f);
        }
        
    }

    /** {@inheritDoc} */
    @Override
    protected void codeGenInst(DecacCompiler compiler) {
        Data data = compiler.getData();
        compiler.addInstruction(new ADDSP(param.size() + 1));
        
        obj.codeGenInst(compiler);
        
        // On calcul l'emplacement de la méthode
        Register addresseClassRegister = data.getLastUsedRegister();
        data.decrementFreeStoragePointer();

        // On reserve la place pour les parametres
        // GPRegister register = data.getFreeRegister(compiler);
        // data.decrementFreeStoragePointer();
        
        // On empile le parametre implicite
        // compiler.addInstruction(new LOAD(addresseClassRegister, register));
        // compiler.addInstruction(new STORE(register, new RegisterOffset(0, Register.SP)));
        // Ajout
        compiler.addInstruction(new STORE(addresseClassRegister, new RegisterOffset(0, Register.SP)));
        
        // On empile les autres parametres
        param.codeGenListExpr(compiler);
        
        // GPRegister register = data.getFreeRegister(compiler);
        GPRegister register = data.getLastUsedRegister();
        // register = data.getFreeRegister(compiler);
        compiler.addInstruction(new LOAD(new RegisterOffset(0, Register.SP), register));
        if (!(compiler.getCompilerOptions().getNoCheck())) {
            compiler.addInstruction(new CMP(new NullOperand(), register));
            compiler.addInstruction(new BEQ(new Label("null_dereference")));
        }
        // On recupere l'adresse de la table des méthodes
        compiler.addInstruction(new LOAD(new RegisterOffset(0, register), register));
        // On saute à la méthode
        compiler.addInstruction(new BSR(new RegisterOffset(meth.getMethodDefinition().getIndex(), register)));
        // On dépile nos parametres
        compiler.addInstruction(new SUBSP(param.size() + 1));
        // Le resultat de la méthode est stocké dans R0
        data.setLastUsedRegister(Register.R0);
    }

    /** {@inheritDoc} */
    @Override
    protected void codeGenSelect(DecacCompiler compiler) {
        codeGenInst(compiler);
    }
}
