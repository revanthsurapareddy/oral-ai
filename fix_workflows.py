import yaml, os

def fix_workflow(filepath):
    print("Checking:", filepath)
    with open(filepath, 'r') as f:
        lines = f.readlines()
    
    new_lines = []
    in_cat = False
    for line in lines:
        if "cat << 'EOF' > runner.py" in line:
            in_cat = True
            new_lines.append(line)
            continue
        if in_cat:
            if line.strip() == "EOF":
                in_cat = False
                new_lines.append("            EOF\n")
            else:
                new_lines.append("            " + line.strip() + "\n")
        else:
            new_lines.append(line)
            
    content = "".join(new_lines)
    try:
        yaml.safe_load(content)
        print(" -> YAML IS 100% VALID!")
        with open(filepath, 'w') as f:
            f.write(content)
    except Exception as e:
        print(" -> YAML ERROR:", e)

fix_workflow(r"d:\project files\oral-ai\.github\workflows\deploy-and-test.yml")
fix_workflow(r"d:\project files\oral-ai\.github\workflows\e2e-tests.yml")
fix_workflow(r"d:\project files\web_project\oral cancer web\.github\workflows\deploy-and-test.yml")
fix_workflow(r"d:\project files\web_project\oral cancer web\.github\workflows\e2e-tests.yml")
