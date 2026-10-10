#!/usr/bin/env python3
"""
Comprehensive script to rewrite pattern matching switch expressions to if-else chains.
Handles both switch expressions (->) and switch statements (:).
"""

import re
import sys
from pathlib import Path

try:
    import javalang
except ImportError:
    print("javalang not installed. Run: pip install javalang")
    sys.exit(1)

class SwitchRewriter:
    def __init__(self, content):
        self.content = content
        self.lines = content.split('\n')
        self.replacements = []  # List of (start_line, end_line, new_lines)
    
    def find_switch_blocks(self):
        """Find all switch blocks with pattern matching."""
        tree = javalang.parse.parse(self.content)
        self._visit_for_switches(tree)
    
    def _visit_for_switches(self, node, parent=None):
        if isinstance(node, javalang.tree.SwitchExpression):
            self._process_switch_expression(node, parent)
        elif isinstance(node, javalang.tree.SwitchStatement):
            self._process_switch_statement(node, parent)
        
        for attr_name in dir(node):
            if attr_name.startswith('_'):
                continue
            attr = getattr(node, attr_name)
            if isinstance(attr, javalang.tree.Node):
                self._visit_for_switches(attr, node)
            elif isinstance(attr, list):
                for item in attr:
                    if isinstance(item, javalang.tree.Node):
                        self._visit_for_switches(item, node)
    
    def _process_switch_expression(self, node, parent):
        """Process a switch expression with pattern matching."""
        # Check if any case has pattern matching
        has_pattern = False
        for case in node.cases:
            if case.pattern and hasattr(case.pattern, 'type') and case.pattern.type:
                has_pattern = True
                break
        
        if not has_pattern:
            return
        
        # Get the switch expression source
        if not hasattr(node, 'position') or not node.position:
            return
        
        start_line = node.position[0]
        
        # Find the end of the switch expression
        end_line = self._find_switch_end(start_line - 1)
        if end_line == -1:
            return
        
        # Generate if-else chain
        new_lines = self._generate_if_else_from_switch(node, is_expression=True)
        if new_lines:
            self.replacements.append((start_line - 1, end_line, new_lines))
    
    def _process_switch_statement(self, node, parent):
        """Process a switch statement with pattern matching."""
        has_pattern = False
        for case in node.cases:
            if case.pattern and hasattr(case.pattern, 'type') and case.pattern.type:
                has_pattern = True
                break
        
        if not has_pattern:
            return
        
        if not hasattr(node, 'position') or not node.position:
            return
        
        start_line = node.position[0]
        end_line = self._find_switch_end(start_line - 1)
        if end_line == -1:
            return
        
        new_lines = self._generate_if_else_from_switch(node, is_expression=False)
        if new_lines:
            self.replacements.append((start_line - 1, end_line, new_lines))
    
    def _find_switch_end(self, start_idx):
        """Find the line index where the switch block ends."""
        brace_count = 0
        found_brace = False
        for i in range(start_idx, len(self.lines)):
            line = self.lines[i]
            for ch in line:
                if ch == '{':
                    brace_count += 1
                    found_brace = True
                elif ch == '}':
                    brace_count -= 1
                    if found_brace and brace_count == 0:
                        return i
        return -1
    
    def _get_indent(self, line_idx):
        """Get indentation of a line."""
        if line_idx < len(self.lines):
            line = self.lines[line_idx]
            match = re.match(r'^(\s*)', line)
            return match.group(1) if match else ''
        return ''
    
    def _generate_if_else_from_switch(self, node, is_expression):
        """Generate if-else chain from switch node."""
        # Get the expression being switched on
        expr_text = self._get_node_text(node.expression)
        if not expr_text:
            return None
        
        indent = self._get_indent(node.position[0] - 1) if hasattr(node, 'position') and node.position else ''
        
        # Determine the result variable if it's an expression
        result_var = None
        if is_expression:
            # Check if this is an assignment or return
            parent_text = self._get_parent_context(node)
            if 'return' in parent_text:
                result_var = 'return'
            elif '=' in parent_text:
                result_var = 'assign'
        
        lines = []
        first = True
        
        for i, case in enumerate(node.cases):
            case_lines = self._generate_case_if(case, expr_text, indent, first, i == len(node.cases) - 1, is_expression, result_var)
            if case_lines:
                lines.extend(case_lines)
                first = False
        
        return lines
    
    def _get_parent_context(self, node):
        """Get context around the node."""
        if hasattr(node, 'position') and node.position:
            line_idx = node.position[0] - 1
            if line_idx > 0:
                return self.lines[line_idx - 1]
        return ''
    
    def _generate_case_if(self, case, expr_text, indent, first, last, is_expression, result_var):
        """Generate if/else if block for a case."""
        lines = []
        
        if case.pattern and hasattr(case.pattern, 'type') and case.pattern.type:
            type_name = self._get_type_name(case.pattern.type)
            var_name = case.pattern.name if hasattr(case.pattern, 'name') else 'it'
            
            if first:
                lines.append(f"{indent}if ({expr_text} instanceof {type_name}) {{")
            else:
                lines.append(f"{indent}else if ({expr_text} instanceof {type_name}) {{")
            
            lines.append(f"{indent}    {type_name} {var_name} = ({type_name}) {expr_text};")
            
            # Process case body
            body_lines = self._process_case_body(case, var_name, indent + "    ", is_expression, result_var)
            lines.extend(body_lines)
            
            lines.append(f"{indent}}}")
            
            if last and not (case.pattern and hasattr(case.pattern, 'type') and case.pattern.type):
                # Default case
                lines.append(f"{indent}else {{")
                body_lines = self._process_case_body(case, var_name, indent + "    ", is_expression, result_var)
                lines.extend(body_lines)
                lines.append(f"{indent}}}")
        
        elif last:  # default case
            if first:
                lines.append(f"{indent}{{")
            else:
                lines.append(f"{indent}else {{")
            
            body_lines = self._process_case_body(case, None, indent + "    ", is_expression, result_var)
            lines.extend(body_lines)
            lines.append(f"{indent}}}")
        
        return lines
    
    def _get_type_name(self, type_node):
        """Extract type name from type node."""
        if hasattr(type_node, 'name'):
            return type_node.name
        return str(type_node)
    
    def _process_case_body(self, case, var_name, indent, is_expression, result_var):
        """Process the body of a case."""
        lines = []
        
        if is_expression:
            # For switch expressions, the case has an expression result
            if hasattr(case, 'expression') and case.expression:
                expr_text = self._get_node_text(case.expression)
                if expr_text:
                    if result_var == 'return':
                        lines.append(f"{indent}return {expr_text};")
                    elif result_var == 'assign':
                        # We can't easily determine the target, so we'll use a temp variable
                        lines.append(f"{indent}__switch_result = {expr_text};")
                    else:
                        lines.append(f"{indent}{expr_text};")
        else:
            # For switch statements, process statements
            if hasattr(case, 'statements'):
                for stmt in case.statements:
                    stmt_text = self._get_node_text(stmt)
                    if stmt_text:
                        lines.append(f"{indent}{stmt_text}")
        
        return lines
    
    def _get_node_text(self, node):
        """Get source text for a node (approximate)."""
        if not hasattr(node, 'position') or not node.position:
            return None
        line, col = node.position
        if line <= len(self.lines):
            return self.lines[line - 1][col - 1:].strip()
        return None
    
    def apply(self):
        """Apply all replacements."""
        # Sort by start line descending to not affect line numbers
        self.replacements.sort(key=lambda x: x[0], reverse=True)
        
        result_lines = self.lines[:]
        for start, end, new_lines in self.replacements:
            result_lines = result_lines[:start] + new_lines + result_lines[end + 1:]
        
        return '\n'.join(result_lines)


def rewrite_file(filepath):
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
    except Exception as e:
        print(f"Error reading {filepath}: {e}")
        return False
    
    try:
        rewriter = SwitchRewriter(content)
        rewriter.find_switch_blocks()
        new_content = rewriter.apply()
    except Exception as e:
        print(f"Error processing {filepath}: {e}")
        import traceback
        traceback.print_exc()
        return False
    
    if new_content != content:
        try:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f"Rewrote: {filepath}")
            return True
        except Exception as e:
            print(f"Error writing {filepath}: {e}")
            return False
    return False


def main():
    java_files = list(Path('android/app/src/main/java').rglob('*.java'))
    fixed_count = 0
    for filepath in java_files:
        try:
            if rewrite_file(filepath):
                fixed_count += 1
        except Exception as e:
            print(f"Error processing {filepath}: {e}")
    print(f"Total files fixed: {fixed_count}")

if __name__ == '__main__':
    main()