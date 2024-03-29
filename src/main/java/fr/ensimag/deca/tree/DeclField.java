package fr.ensimag.deca.tree;

import fr.ensimag.deca.context.Type;
import fr.ensimag.deca.DecacCompiler;
import fr.ensimag.deca.context.ClassDefinition;
import fr.ensimag.deca.context.ClassType;
import fr.ensimag.deca.context.ContextualError;
import fr.ensimag.deca.context.EnvironmentExp;
import fr.ensimag.deca.context.EnvironmentType;
import fr.ensimag.deca.context.ExpDefinition;
import fr.ensimag.deca.context.FieldDefinition;
import fr.ensimag.deca.tools.IndentPrintStream;
import fr.ensimag.ima.pseudocode.Register;
import fr.ensimag.ima.pseudocode.RegisterOffset;
import fr.ensimag.ima.pseudocode.instructions.LOAD;
import fr.ensimag.ima.pseudocode.instructions.STORE;
import fr.ensimag.deca.tools.SymbolTable.Symbol;
import java.io.PrintStream;
import org.apache.commons.lang.Validate;

/**
 * Declaration of a field
 *
 * @author oscarmaggiori
 * @version $Id: $Id
 */
public class DeclField extends AbstractDeclField {

    private Visibility v;
    private AbstractIdentifier type;
    private AbstractIdentifier field;
    private AbstractInitialization init;
    

    /**
     * <p>Constructor for DeclField.</p>
     *
     * @param v a {@link fr.ensimag.deca.tree.Visibility} object
     * @param type a {@link fr.ensimag.deca.tree.AbstractIdentifier} object
     * @param field a {@link fr.ensimag.deca.tree.AbstractIdentifier} object
     * @param init a {@link fr.ensimag.deca.tree.AbstractInitialization} object
     */
    public DeclField(Visibility v, AbstractIdentifier type, AbstractIdentifier field, AbstractInitialization init){
        Validate.notNull(v);
        Validate.notNull(type);
        Validate.notNull(field);
        Validate.notNull(init);
        this.v = v;
        this.type = type;
        this.field = field;
        this.init = init;
    }

    /**
     * <p>Constructor for DeclField.</p>
     *
     * @param tree a {@link fr.ensimag.deca.tree.AbstractIdentifier} object
     * @param tree2 a {@link fr.ensimag.deca.tree.AbstractExpr} object
     * @param init2 a {@link fr.ensimag.deca.tree.AbstractInitialization} object
     */
    public DeclField(AbstractIdentifier tree, AbstractExpr tree2, AbstractInitialization init2) {
    }

    /**
     * <p>Getter for the field <code>field</code>.</p>
     *
     * @return a {@link fr.ensimag.deca.tree.AbstractIdentifier} object
     */
    public AbstractIdentifier getField(){
        return this.field;
    }

    /** {@inheritDoc} */
    @Override
    public void verifyField(DecacCompiler compiler, AbstractIdentifier classeSup, AbstractIdentifier classe)
    throws ContextualError {
        Symbol fieldName = this.field.getName();
        EnvironmentType env_Types = compiler.GetEnvTypes();
        Symbol symbSup = classeSup.getName();
        ClassType classTypeSup = (ClassType) env_Types.getType(symbSup);
        Symbol symb = classe.getName();
        ClassType defType = (ClassType) env_Types.getType(symb);
        if (classTypeSup != null && defType != null) {
            ClassDefinition classDefSup = classTypeSup.getDefinition();
            EnvironmentExp envExpSup = classDefSup.getMembers();
            ClassDefinition def = defType.getDefinition();
            EnvironmentExp envExp = def.getMembers();
            if (envExpSup.equals(envExp.getParent())) {
                ExpDefinition ExpDef = envExpSup.get(fieldName);
                if (ExpDef != null) {
                    if (!ExpDef.isField()) {
                        throw new ContextualError(fieldName.getName() + " isn't a field", this.type.getLocation());
                    }
                }
            }
            Type fType = type.verifyType(compiler);
            if (fType.isVoid()){
                throw new ContextualError("field void", this.type.getLocation());
            }
            try {
                int index = classDefSup.getNumberOfFields() + def.getNumberOfFields() + 1;
                FieldDefinition newDef= new FieldDefinition(fType, field.getLocation(), v, null, index);
                field.setDefinition(newDef);
                field.setType(fType);
                envExp.declare(fieldName, newDef);
                def.incNumberOfFields();
                init.verifyInitialization(compiler, fType, envExp , def);
            } catch (EnvironmentExp.DoubleDefException e) {
                String message = "can't defined field identifier several times in a class";
                throw new ContextualError(message, field.getLocation());
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void verifyFieldBody(DecacCompiler compiler, AbstractIdentifier classeSup, AbstractIdentifier classe)
    throws ContextualError {
        Symbol fieldName = this.field.getName();
        EnvironmentType env_Types = compiler.GetEnvTypes();
        Symbol symbSup = classeSup.getName();
        ClassType classTypeSup = (ClassType) env_Types.getType(symbSup);
        Symbol symb = classe.getName();
        ClassType defType = (ClassType) env_Types.getType(symb);
        if (classTypeSup != null && defType != null) {
            ClassDefinition classDefSup = classTypeSup.getDefinition();
            EnvironmentExp envExpSup = classDefSup.getMembers();
            ClassDefinition def = defType.getDefinition();
            EnvironmentExp envExp = def.getMembers();
            if (envExpSup.equals(envExp.getParent())) {
                ExpDefinition ExpDef = envExpSup.get(fieldName);
                if (ExpDef != null) {
                    if (!compiler.subType(compiler, this.type.getType(), ExpDef.getType())) {
                        throw new ContextualError("Incompatible extension of field "+this.field.getName(), this.getLocation());
                    }
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void decompile(IndentPrintStream s) {
        if (v.toString().equals("PROTECTED")) {
            s.print("protected");
        }
        s.print(" ");
        type.decompile(s);
        s.print(" ");
    	this.field.decompile(s);
    	s.print(" ");
    	this.init.decompile(s);
    	s.println(";");	
    }

    /** {@inheritDoc} */
    @Override
    protected void prettyPrintChildren(PrintStream s, String prefix) {
        this.type.prettyPrint(s, prefix, false);
        this.field.prettyPrint(s, prefix, false);
        this.init.prettyPrint(s, prefix, true);
        
    }

    /** {@inheritDoc} */
    @Override
    protected void iterChildren(TreeFunction f) {
        type.iter(f);
        field.iter(f);
        init.iter(f);
    }

    /** {@inheritDoc} */
    @Override
    protected void codeGenDeclField(DecacCompiler compiler) {
        compiler.addComment("Initialisation du champ " + field.getName().getName());
        init.codeGenInitField(compiler, field.getType());
        compiler.addInstruction(new LOAD(new RegisterOffset(-2, Register.LB), Register.R1));
        compiler.addInstruction(new STORE(Register.R0, new RegisterOffset(field.getFieldDefinition().getIndex(), Register.R1)));
    }

    /** {@inheritDoc} */
    @Override
    protected void codeGenDeclFieldZero(DecacCompiler compiler) {
        compiler.addComment("Initialisation du champ " + field.getName().getName() + " à zero");
        compiler.addInstruction(new LOAD(0, Register.R0));
        compiler.addInstruction(new LOAD(new RegisterOffset(-2, Register.LB), Register.R1));
        compiler.addInstruction(new STORE(Register.R0, new RegisterOffset(field.getFieldDefinition().getIndex(), Register.R1)));
    }
    
}
