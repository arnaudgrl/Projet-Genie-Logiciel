package fr.ensimag.deca.tree;

import org.apache.commons.lang.Validate;

import fr.ensimag.deca.context.ClassDefinition;
import fr.ensimag.deca.context.ClassType;
import fr.ensimag.deca.DecacCompiler;
import fr.ensimag.deca.context.ContextualError;
import fr.ensimag.deca.context.EnvironmentExp;
import fr.ensimag.deca.context.EnvironmentType;
import fr.ensimag.deca.context.ExpDefinition;
import fr.ensimag.deca.context.MethodDefinition;
import fr.ensimag.deca.context.Signature;
import fr.ensimag.deca.context.Type;
import fr.ensimag.deca.tools.IndentPrintStream;
import fr.ensimag.ima.pseudocode.instructions.BOV;
import fr.ensimag.ima.pseudocode.instructions.ERROR;
import fr.ensimag.ima.pseudocode.instructions.TSTO;
import fr.ensimag.ima.pseudocode.instructions.WNL;
import fr.ensimag.ima.pseudocode.instructions.WSTR;
import fr.ensimag.ima.pseudocode.Label;
import fr.ensimag.deca.tools.SymbolTable.Symbol;


import java.io.PrintStream;

/**
 * Declaration of a method
 *
 * @author oscarmaggiori
 * @version $Id: $Id
 */
public class DeclMethod extends AbstractDeclMethod{

    private AbstractIdentifier type;
    private AbstractIdentifier name;
    private ListDeclParam listDeclParam;
    private AbstractMethodBody methodBody;

    /**
     * <p>Constructor for DeclMethod.</p>
     *
     * @param type a {@link fr.ensimag.deca.tree.AbstractIdentifier} object
     * @param name a {@link fr.ensimag.deca.tree.AbstractIdentifier} object
     * @param listDeclParam a {@link fr.ensimag.deca.tree.ListDeclParam} object
     * @param methodBody a {@link fr.ensimag.deca.tree.AbstractMethodBody} object
     */
    public DeclMethod(AbstractIdentifier type, AbstractIdentifier name, ListDeclParam listDeclParam, AbstractMethodBody methodBody){
        Validate.notNull(type);
        Validate.notNull(name);
        Validate.notNull(listDeclParam);
        this.type = type;
        this.name = name;
        this.listDeclParam = listDeclParam;
        this.methodBody = methodBody;
    }

    /** {@inheritDoc} */
    @Override
    protected void verifyMethod(DecacCompiler compiler, AbstractIdentifier classeSup, AbstractIdentifier classe)
            throws ContextualError{
        
        Symbol method = this.name.getName();
        Type mType = this.type.verifyType(compiler);
        EnvironmentType env_Types = compiler.GetEnvTypes();
        Symbol symbSup = classeSup.getName();
        ClassType classTypeSup = (ClassType) env_Types.getType(symbSup);
        Signature sig2 = this.listDeclParam.verifyListParam(compiler);
        if (classTypeSup != null) {
            ClassDefinition classDefSup = classTypeSup.getDefinition();
            EnvironmentExp envExpSup = classDefSup.getMembers();
            ExpDefinition ExpDef = envExpSup.get(method);
            Symbol symb = classe.getName();
            ClassType classType = (ClassType) env_Types.getType(symb);
            ClassDefinition classDef = classType.getDefinition();
            int index = classDef.getIndexMethods();
            if (ExpDef != null) {
                if (!ExpDef.isMethod()) {
                    throw new ContextualError(method.getName() + " isn't a method", this.getLocation());
                }
                MethodDefinition methodDef = (MethodDefinition) ExpDef;
                Type typeSup = methodDef.getType();
                if (compiler.subType(compiler, mType, typeSup)) {
                    Signature sig = methodDef.getSignature();
                    if (sig.sameSignature(compiler, sig2)) {
                        index = methodDef.getIndex();
                    } else {
                        throw new ContextualError(method.getName()+" must have same signature", this.getLocation());
                    }
                } else {
                    throw new ContextualError(method.getName()+" must have same type", this.getLocation());
                }
            } else {
                classDef.incIndexMethods();
            }
            try {
                EnvironmentExp envExp = classDef.getMembers();
                MethodDefinition newDef = new MethodDefinition(mType, name.getLocation(), sig2, index);
                Label label = new Label(symb.getName() +"."+ method.getName());
                newDef.setLabel(label);
                name.setDefinition(newDef);
                name.setType(mType);
                envExp.declare(method, newDef);
                classDef.incNumberOfMethods();
            } catch (EnvironmentExp.DoubleDefException e) {
                String message = "can't defined method identifier several times in a class";
                throw new ContextualError(message, name.getLocation());
            }
        } else {
            throw new ContextualError(symbSup.getName()+" can't have null type", classeSup.getLocation());
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void verifyMethodBody(DecacCompiler compiler, AbstractIdentifier classeSup, AbstractIdentifier classe)
            throws ContextualError{
                ClassDefinition currentClass = classe.getClassDefinition();
                EnvironmentExp EnvExpClass = currentClass.getMembers();
                MethodDefinition nameDef = name.getMethodDefinition();
                EnvironmentExp localEnvInit = nameDef.getMembers();
                this.listDeclParam.verifyListDeclParam(compiler, localEnvInit);
                EnvironmentExp localEnv = localEnvInit.empilement(EnvExpClass);
                this.methodBody.verifyMethodBody(compiler, localEnv, currentClass, this.type.getType());
            }

    /** {@inheritDoc} */
    @Override
    public void decompile(IndentPrintStream s) {
        type.decompile(s);
        s.print(" ");
        name.decompile(s);
        s.print("(");
        listDeclParam.decompile(s);
        s.print(") ");
        methodBody.decompile(s);
    }

    /** {@inheritDoc} */
    @Override
    protected void prettyPrintChildren(PrintStream s, String prefix) {
        this.type.prettyPrint(s, prefix, false);
        this.name.prettyPrint(s, prefix, false);
        this.listDeclParam.prettyPrint(s, prefix, false);
        this.methodBody.prettyPrint(s, prefix, true);
        
    }

    /** {@inheritDoc} */
    @Override
    protected void iterChildren(TreeFunction f) {
        type.iter(f);
        name.iter(f);
        listDeclParam.iter(f);
        
    }
    /** {@inheritDoc} */
    @Override
    public AbstractIdentifier getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof DeclMethod) {
            DeclMethod other = (DeclMethod)obj;
            return other.getName().getName().getName().equals(this.getName().getName().getName());
        }
        return false;
    }
    
    /** {@inheritDoc} */
    public void codeGenDeclMethod(DecacCompiler compiler) {
        // Début de bloc
        compiler.newBloc(); compiler.setToBlocProgram();
        Label labelReturn = new Label(
            "fin." + name.getMethodDefinition().getLabel().toString()
        );
        compiler.getData().setLabelReturn(labelReturn);

        // Calcul de methodBody
        /**
         * TODO : le nmbre maximal de paramètres des méthodes appelees
         */
        methodBody.codeGenMethodBody(compiler);

        if (!(name.getMethodDefinition().getType().isVoid())) {
            if (!(compiler.getCompilerOptions().getNoCheck())) {
                compiler.addInstruction(
                    new WSTR(
                        "Error: method " + name.getMethodDefinition().getLabel().toString() + " needs a return"
                    )
                );
            }
            compiler.addInstruction(new WNL());
            compiler.addInstruction(new ERROR());
        }

        compiler.addLabel(labelReturn);

        // Restauration et sauvegarde des registres si besoin
        methodBody.codeGenSaveRestore(compiler);

        // TSTO
        /**
         * TODO : TSTO
         * 
         * nombre de registres sauvegardés en début de bloc = 
         *          compiler.getData().getNumberOfUsedRegister()
         * nombre de variables du bloc =
         *          ?
         * nombre maximal de temporaires nécessaires à l’évaluation des expressions =
         *          compiler.getData().getMaxStackLength()
         * nombre maximal de paramètres des méthodes appelées (chaque instruction BSR effectuant deux empilements) =
         *          2 <- Il faut retenir le PC et l'objet
         * 
         */
        if (!(compiler.getCompilerOptions().getNoCheck())) {
            compiler.addInstructionAtFirst(new BOV(new Label("stack_overflow_error")));
            compiler.addInstructionAtFirst(
                new TSTO(
                    compiler.getData().getNumberOfUsedRegister() +
                    compiler.getData().getMaxStackLength() +
                    ((AbstractMethodBody)methodBody).getNbrVarMethodBody()
                )
            );
        }
        compiler.addLabelAtFirst(new Label("code."+name.getMethodDefinition().getLabel().toString()));

        // Fin de bloc 
        compiler.appendBlocInstructions();
        compiler.getData().restoreData();
        compiler.setToMainProgram();

    }   
}
